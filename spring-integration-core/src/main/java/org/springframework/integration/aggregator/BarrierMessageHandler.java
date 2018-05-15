/*
 * Copyright 2015-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.aggregator;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.handler.DiscardingMessageHandler;
import org.springframework.integration.handler.MessageTriggerAction;
import org.springframework.integration.store.SimpleMessageGroup;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;

/**
 * A message handler that suspends the thread until a message with corresponding
 * correlation is passed into the {@link #trigger(Message) trigger} method or
 * the timeout occurs. Only one thread with a particular correlation (result of invoking
 * the {@link CorrelationStrategy}) can be suspended at a time. If the inbound thread does
 * not arrive before the trigger thread, the latter is suspended until it does, or the
 * timeout occurs.
 * <p>
 * The default {@link CorrelationStrategy} is a {@link HeaderAttributeCorrelationStrategy}.
 * <p>
 * The default output processor is a {@link DefaultAggregatingMessageGroupProcessor}.
 *
 * @author Gary Russell
 *
 * @since 4.2
 */
public class BarrierMessageHandler extends AbstractReplyProducingMessageHandler
		implements MessageTriggerAction, DiscardingMessageHandler {

	private final Map<Object, SynchronousQueue<Message<?>>> suspensions = new ConcurrentHashMap<>();

	private final Map<Object, Thread> inProcess = new ConcurrentHashMap<>();

	private final long timeout;

	private final CorrelationStrategy correlationStrategy;

	private final MessageGroupProcessor messageGroupProcessor;

	private volatile MessageChannel discardChannel;

	private String discardChannelName;

	/**
	 * Construct an instance with the provided timeout and default correlation and
	 * output strategies.
	 * @param timeout the timeout in milliseconds.
	 */
	public BarrierMessageHandler(long timeout) {
		this(timeout, new DefaultAggregatingMessageGroupProcessor());
	}

	/**
	 * Construct an instance with the provided timeout and output processor, and default
	 * correlation strategy.
	 * @param timeout the timeout in milliseconds.
	 * @param outputProcessor the output {@link MessageGroupProcessor}.
	 */
	public BarrierMessageHandler(long timeout, MessageGroupProcessor outputProcessor) {
		this(timeout, outputProcessor, null);
	}

	/**
	 * Construct an instance with the provided timeout and correlation strategy, and default
	 * output processor.
	 * @param timeout the timeout in milliseconds.
	 * @param correlationStrategy the correlation strategy.
	 */
	public BarrierMessageHandler(long timeout, CorrelationStrategy correlationStrategy) {
		this(timeout, new DefaultAggregatingMessageGroupProcessor(), correlationStrategy);
	}

	/**
	 * Construct an instance with the provided timeout and output processor, and default
	 * correlation strategy.
	 * @param timeout the timeout in milliseconds.
	 * @param outputProcessor the output {@link MessageGroupProcessor}.
	 * @param correlationStrategy the correlation strategy.
	 */
	public BarrierMessageHandler(long timeout, MessageGroupProcessor outputProcessor,
			CorrelationStrategy correlationStrategy) {

		Assert.notNull(outputProcessor, "'messageGroupProcessor' cannot be null");
		this.messageGroupProcessor = outputProcessor;
		this.correlationStrategy = (correlationStrategy == null
				? new HeaderAttributeCorrelationStrategy(IntegrationMessageHeaderAccessor.CORRELATION_ID)
				: correlationStrategy);
		this.timeout = timeout;
	}

	/**
	 * Set the name of the channel to which late arriving trigger messages are sent.
	 * @param discardChannelName the discard channel.
	 * @since 5.0
	 */
	public void setDiscardChannelName(String discardChannelName) {
		this.discardChannelName = discardChannelName;
	}

	/**
	 * Set the channel to which late arriving trigger messages are sent.
	 * @param discardChannel the discard channel.
	 * @since 5.0
	 */
	public void setDiscardChannel(MessageChannel discardChannel) {
		this.discardChannel = discardChannel;
	}

	/**
	 * @since 5.0
	 */
	@Override
	public MessageChannel getDiscardChannel() {
		if (this.discardChannel == null && this.discardChannelName != null && getChannelResolver() != null) {
			this.discardChannel = getChannelResolver().resolveDestination(this.discardChannelName);
		}
		return this.discardChannel;
	}

	@Override
	public String getComponentType() {
		return "barrier";
	}

	@Override
	protected Object handleRequestMessage(Message<?> requestMessage) {
		Object key = this.correlationStrategy.getCorrelationKey(requestMessage);
		if (key == null) {
			throw new MessagingException(requestMessage, "Correlation Strategy returned null");
		}
		Thread existing = this.inProcess.putIfAbsent(key, Thread.currentThread());
		if (existing != null) {
			throw new MessagingException(requestMessage, "Correlation key ("
					+ key + ") is already in use by " + existing.getName());
		}
		SynchronousQueue<Message<?>> syncQueue = createOrObtainQueue(key);
		try {
			Message<?> releaseMessage = syncQueue.poll(this.timeout, TimeUnit.MILLISECONDS);
			if (releaseMessage != null) {
				return processRelease(key, requestMessage, releaseMessage);
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new MessageHandlingException(requestMessage, "Interrupted while waiting for release", e);
		}
		finally {
			this.inProcess.remove(key);
			this.suspensions.remove(key);
		}
		return null;
	}

	private Object processRelease(Object key, Message<?> requestMessage, Message<?> releaseMessage) {
		this.suspensions.remove(key);
		if (releaseMessage.getPayload() instanceof Throwable) {
			throw new MessagingException(requestMessage, "Releasing flow returned a throwable",
					(Throwable) releaseMessage.getPayload());
		}
		else {
			return buildResult(key, requestMessage, releaseMessage);
		}
	}

	/**
	 * Override to change the default mechanism by which the inbound and release messages
	 * are returned as a result.
	 * @param key The correlation key.
	 * @param requestMessage the inbound message.
	 * @param releaseMessage the release message.
	 * @return the result.
	 */
	protected Object buildResult(Object key, Message<?> requestMessage, Message<?> releaseMessage) {
		SimpleMessageGroup group = new SimpleMessageGroup(key);
		group.add(requestMessage);
		group.add(releaseMessage);
		return this.messageGroupProcessor.processMessageGroup(group);
	}

	private SynchronousQueue<Message<?>> createOrObtainQueue(Object key) {
		SynchronousQueue<Message<?>> syncQueue = new SynchronousQueue<>();
		SynchronousQueue<Message<?>> existing = this.suspensions.putIfAbsent(key, syncQueue);
		if (existing != null) {
			syncQueue = existing;
		}
		return syncQueue;
	}

	@Override
	public void trigger(Message<?> message) {
		Object key = this.correlationStrategy.getCorrelationKey(message);
		if (key == null) {
			throw new MessagingException(message, "Correlation Strategy returned null");
		}
		SynchronousQueue<Message<?>> syncQueue = createOrObtainQueue(key);
		try {
			if (!syncQueue.offer(message, this.timeout, TimeUnit.MILLISECONDS)) {
				this.logger.error("Suspending thread timed out or did not arrive within timeout for: " + message);
				this.suspensions.remove(key);
				if (getDiscardChannel() != null) {
					this.messagingTemplate.send(getDiscardChannel(), message);
				}
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			this.logger.error("Interrupted while waiting for the suspending thread for: " + message);
			this.suspensions.remove(key);
		}
	}

}
