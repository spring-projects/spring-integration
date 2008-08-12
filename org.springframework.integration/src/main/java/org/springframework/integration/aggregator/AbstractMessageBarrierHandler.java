/*
 * Copyright 2002-2008 the original author or authors.
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

import java.util.List;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.message.BlockingTarget;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.integration.message.MessageTarget;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * Base class for {@link MessageBarrier}-based MessageHandlers.
 * A {@link MessageHandler} implementation that waits for a group of
 * {@link Message Messages} to arrive and processes them together.
 * Uses a {@link MessageBarrier} to store messages and to decide how
 * the messages should be released.
 * <p>
 * Each {@link Message} that is received by this handler will be associated with
 * a group based upon the '<code>correlationId</code>' property of its
 * header. If no such property is available, a {@link MessageHandlingException}
 * will be thrown.
 * <p>
 * The '<code>timeout</code>' value determines how long to wait for the
 * complete group after the arrival of the first {@link Message} of the group.
 * The default value is 1 minute. If the timeout elapses prior to completion,
 * then Messages with that timed-out 'correlationId' will be sent to the
 * 'discardChannel' if provided.
 * 
 * @author Mark Fisher
 * @author Marius Bogoevici
 */
public abstract class AbstractMessageBarrierHandler implements MessageHandler, InitializingBean {

	public final static long DEFAULT_SEND_TIMEOUT = 1000;

	public final static long DEFAULT_TIMEOUT = 60000;

	public final static long DEFAULT_REAPER_INTERVAL = 1000;

	public final static int DEFAULT_TRACKED_CORRRELATION_ID_CAPACITY = 1000;

	protected final Log logger = LogFactory.getLog(this.getClass());

	protected volatile MessageChannel outputChannel;

	private volatile MessageChannel discardChannel;

	protected volatile long sendTimeout = DEFAULT_SEND_TIMEOUT;

	protected final ConcurrentMap<Object, MessageBarrier> barriers =
			new ConcurrentHashMap<Object, MessageBarrier>();

	private volatile long timeout = DEFAULT_TIMEOUT;

	private volatile boolean sendPartialResultOnTimeout = false;

	private volatile long reaperInterval = DEFAULT_REAPER_INTERVAL;

	private volatile int trackedCorrelationIdCapacity = DEFAULT_TRACKED_CORRRELATION_ID_CAPACITY;

	protected volatile BlockingQueue<Object> trackedCorrelationIds;

	protected final ScheduledExecutorService executor;

	private volatile boolean initialized;


	public AbstractMessageBarrierHandler(ScheduledExecutorService executor) {
		this.executor = (executor != null) ? executor : Executors.newSingleThreadScheduledExecutor();		
	}


	/**
	 * Set the output channel for sending aggregated Messages. Note that
	 * precedence will be given to the 'returnAddress' of the aggregated
	 * message itself, then to the 'returnAddress' of the original message.
	 */
	public void setOutputChannel(MessageChannel outputChannel) {
		this.outputChannel = outputChannel;
	}

	/**
	 * Specify a channel for sending Messages that arrive after their aggregation
	 * group has either completed or timed-out.
	 */
	public void setDiscardChannel(MessageChannel discardChannel) {
		this.discardChannel = discardChannel;
	}

	/**
	 * Set the timeout for sending aggregation results and discarded Messages.
	 */
	public void setSendTimeout(long sendTimeout) {
		this.sendTimeout = sendTimeout;
	}

	/**
	 * Specify whether to aggregate and send the resulting Message when the
	 * timeout elapses prior to the CompletionStrategy.
	 */
	public void setSendPartialResultOnTimeout(boolean sendPartialResultOnTimeout) {
		this.sendPartialResultOnTimeout = sendPartialResultOnTimeout;
	}

	/**
	 * Set the interval in milliseconds for the reaper thread. Default is 1000.
	 */
	public void setReaperInterval(long reaperInterval) {
		Assert.isTrue(reaperInterval > 0, "'reaperInterval' must be a positive value");
		this.reaperInterval = reaperInterval;
	}

	/**
	 * Set the number of completed correlationIds to track. Default is 1000.
	 */
	public void setTrackedCorrelationIdCapacity(int trackedCorrelationIdCapacity) {
		Assert.isTrue(trackedCorrelationIdCapacity > 0, "'trackedCorrelationIdCapacity' must be a positive value");
		this.trackedCorrelationIdCapacity = trackedCorrelationIdCapacity;
	}

	/**
	 * Initialize this handler.
	 */
	public void afterPropertiesSet() {
		this.trackedCorrelationIds = new ArrayBlockingQueue<Object>(this.trackedCorrelationIdCapacity);
		this.executor.scheduleWithFixedDelay(new ReaperTask(),
				this.reaperInterval, this.reaperInterval, TimeUnit.MILLISECONDS);
		this.initialized = true;
	}

	/**
	 * Maximum time to wait (in milliseconds) for the completion strategy to
	 * become true. The default is 60000 (1 minute).
	 */
	public void setTimeout(long timeout) {
		Assert.isTrue(timeout >= 0, "'timeout' must not be negative");
		this.timeout = timeout;
	}

	public Message<?> handle(Message<?> message) {
		if (!this.initialized) {
			this.afterPropertiesSet();
		}
		Object correlationId = message.getHeaders().getCorrelationId();
		if (correlationId == null) {
			throw new MessageHandlingException(message,
					this.getClass().getSimpleName() + " requires the 'correlationId' property");
		}
		if (this.trackedCorrelationIds.contains(correlationId)) {
			if (logger.isDebugEnabled()) {
				logger.debug("Handling for correlationId '" + correlationId +
						"' has already completed or timed out.");
			}
			this.sendToDiscardChannelIfAvailable(message);
			return null;
		}
		MessageBarrier barrier = barriers.putIfAbsent(correlationId, createMessageBarrier());
		if (barrier == null) {
			barrier = barriers.get(correlationId);
		}
		List<Message<?>> releasedMessages = barrier.addAndRelease(message);
		if (CollectionUtils.isEmpty(releasedMessages)) {
			return null;
		}
		if (isBarrierRemovable(correlationId, releasedMessages)) {
			this.removeBarrier(correlationId);
		}
		afterRelease(correlationId, releasedMessages);
		return null;
	}

	private void afterRelease(Object correlationId, List<Message<?>> releasedMessages) {
		Message<?>[] processedMessages = this.processReleasedMessages(correlationId, releasedMessages);
		for (Message<?> result : processedMessages) {
			MessageTarget replyTarget = this.outputChannel;
			if (replyTarget == null) {
				replyTarget = this.resolveReplyTargetFromMessage(result);
				if (replyTarget == null) {
					replyTarget = this.resolveReplyTargetFromMessage(releasedMessages.get(0));
				}
			}
			if (replyTarget != null) {
				if (replyTarget instanceof BlockingTarget && this.sendTimeout >= 0) {
					((BlockingTarget) replyTarget).send(result, this.sendTimeout);
				}
				else {
					replyTarget.send(result);
				}
			}
			else if (logger.isWarnEnabled()) {
				logger.warn("unable to determine reply target for aggregation result: " + result);
			}
		}
	}

	private void sendToDiscardChannelIfAvailable(Message<?> message) {
		if (this.discardChannel != null) {
			if (!this.discardChannel.send(message, this.sendTimeout)) {
				if (logger.isWarnEnabled()) {
					logger.warn("unable to send to 'discardChannel', message: " + message);
				}
			}
		}
	}

	protected MessageTarget resolveReplyTargetFromMessage(Message<?> message) {
		Object returnAddress = message.getHeaders().getReturnAddress();
		if (returnAddress != null) {
			if (returnAddress instanceof MessageTarget) {
				return (MessageTarget) returnAddress;
			}
			if (logger.isWarnEnabled()) {
				logger.warn("Aggregator can only reply to a 'returnAddress' of type MessageTarget.");
			}
		}
		return null;
	}

	private void removeBarrier(Object correlationId) {
		if (this.barriers.remove(correlationId) != null) {
			synchronized (this.trackedCorrelationIds) {
				boolean added = this.trackedCorrelationIds.offer(correlationId);
				if (!added) {
					this.trackedCorrelationIds.poll();
					this.trackedCorrelationIds.offer(correlationId);
				}
			}
		}
	}


	private class ReaperTask implements Runnable {

		public void run() {
			long currentTime = System.currentTimeMillis();
			for (Map.Entry<Object, MessageBarrier> entry : barriers.entrySet()) {
				if (currentTime - entry.getValue().getTimestamp() >= timeout) {
					Object correlationId = entry.getKey();
					List<Message<?>> messages = entry.getValue().getMessages();
					removeBarrier(correlationId);
					if (sendPartialResultOnTimeout) {
						afterRelease(correlationId, messages);
					}
					else {
						for (Message<?> message : messages) {
							sendToDiscardChannelIfAvailable(message);
						}
					}
				}
			}
		}
	}

	/**
	 * Factory method for creating a suitable MessageBarrier
	 * implementation.
	 */
	protected abstract MessageBarrier createMessageBarrier();

	/**
	 * Implements the logic for deciding whether, based on what the
	 * MessageBarrier has released so far, work for the correlationId
	 * can be considered done and the barrier can be released.
	 */
	protected abstract boolean isBarrierRemovable(Object correlationId, List<Message<?>> releasedMessages);

	/**
	 * Implements the logic for transforming the released Messages.
	 */
	protected abstract Message<?>[] processReleasedMessages(Object correlationId, List<Message<?>> messages);

}
