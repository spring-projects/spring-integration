/*
 * Copyright 2015 the original author or authors.
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
package org.springframework.integration.handler;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.aggregator.CorrelationStrategy;
import org.springframework.integration.aggregator.DefaultAggregatingMessageGroupProcessor;
import org.springframework.integration.aggregator.HeaderAttributeCorrelationStrategy;
import org.springframework.integration.aggregator.MessageGroupProcessor;
import org.springframework.integration.store.SimpleMessageGroup;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.util.Assert;

/**
 * A message handler that suspends the thread until a message with corresponding
 * correlation is passed into the {@link #triggerAction(Message) triggerAction} method or
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
 * @since 4.2
 *
 */
public class BarrierMessageHandler extends AbstractReplyProducingMessageHandler implements TriggerMessageHandler {

	private final ConcurrentMap<Object, SynchronousQueue<Message<?>>> suspensions =
			new ConcurrentHashMap<Object, SynchronousQueue<Message<?>>>();

	private final ConcurrentMap<Object, Thread> inProcess = new ConcurrentHashMap<Object, Thread>();

	private final long timeout;

	private final CorrelationStrategy correlationStrategy;

	private final MessageGroupProcessor messageGroupProcessor;

	/**
	 * Construct an instance with the provided timeout and default correlation and
	 * output strategies.
	 * @param timeout the timeout in milliseconds.
	 */
	public BarrierMessageHandler(long timeout) {
		this(new HeaderAttributeCorrelationStrategy(IntegrationMessageHeaderAccessor.CORRELATION_ID),
				new DefaultAggregatingMessageGroupProcessor(), timeout);
	}

	/**
	 * Construct an instance with the provided timeout and output processor, and default
	 * correlation strategy.
	 * @param timeout the timeout in milliseconds.
	 * @param outputProcessor the output {@link MessageGroupProcessor}.
	 */
	public BarrierMessageHandler(long timeout, MessageGroupProcessor outputProcessor) {
		this(new HeaderAttributeCorrelationStrategy(IntegrationMessageHeaderAccessor.CORRELATION_ID),
				outputProcessor, timeout);
	}

	/**
	 * Construct an instance with the provided timeout and correlation strategy, and default
	 * output processor.
	 * @param timeout the timeout in milliseconds.
	 * @param correlationStrategy the correlation strategy.
	 */
	public BarrierMessageHandler(CorrelationStrategy correlationStrategy, long timeout) {
		this(correlationStrategy, new DefaultAggregatingMessageGroupProcessor(), timeout);
	}

	/**
	 * Construct an instance with the provided timeout and output processor, and default
	 * correlation strategy.
	 * @param timeout the timeout in milliseconds.
	 * @param correlationStrategy the correlation strategy.
	 * @param outputProcessor the output {@link MessageGroupProcessor}.
	 */
	public BarrierMessageHandler(CorrelationStrategy correlationStrategy,
			MessageGroupProcessor outputProcessor, long timeout) {
		Assert.notNull(correlationStrategy, "'correlationStrategy' cannot be null");
		Assert.notNull(outputProcessor, "'messageGroupProcessor' cannot be null");
		this.correlationStrategy = correlationStrategy;
		this.messageGroupProcessor =  outputProcessor;
		this.timeout = timeout;
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
			Message<?> release = syncQueue.poll(this.timeout, TimeUnit.MILLISECONDS);
			if (release != null) {
				return processRelease(requestMessage, key, release);
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new MessagingException(requestMessage, "Interrupted while waiting for release", e);
		}
		finally {
			this.inProcess.remove(key);
			this.suspensions.remove(key);
		}
		return null;
	}

	private Object processRelease(Message<?> requestMessage, Object key, Message<?> release) {
		this.suspensions.remove(key);
		if (release.getPayload() instanceof Throwable) {
			throw new MessagingException(requestMessage, "Releasing flow returned a throwable",
					(Throwable) release.getPayload());
		}
		else {
			return buildResult(key, requestMessage, release);
		}
	}

	/**
	 * Override to change the default mechanism by which the inbound and release messages
	 * are returned as a result.
	 * @param key The correlation key.
	 * @param requestMessage the inbound message.
	 * @param release the release message.
	 * @return the result.
	 */
	protected Object buildResult(Object key, Message<?> requestMessage, Message<?> release) {
		SimpleMessageGroup group = new SimpleMessageGroup(key);
		group.add(requestMessage);
		group.add(release);
		return this.messageGroupProcessor.processMessageGroup(group);
	}

	private SynchronousQueue<Message<?>> createOrObtainQueue(Object key) {
		SynchronousQueue<Message<?>> syncQueue = new SynchronousQueue<Message<?>>();
		SynchronousQueue<Message<?>> existing = this.suspensions.putIfAbsent(key, syncQueue);
		if (existing != null) {
			syncQueue = existing;
		}
		return syncQueue;
	}

	@Override
	public void triggerAction(Message<?> message) {
		Object key = this.correlationStrategy.getCorrelationKey(message);
		if (key == null) {
			throw new MessagingException(message, "Correlation Strategy returned null");
		}
		SynchronousQueue<Message<?>> syncQueue = createOrObtainQueue(key);
		try {
			if (syncQueue != null && !syncQueue.offer(message, timeout, TimeUnit.MILLISECONDS)) {
				this.logger.error("Suspending thread timed out or did not arrive within timeout for: " + message);
				this.suspensions.remove(key);
			}
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new MessagingException(message, e);
		}
	}

}
