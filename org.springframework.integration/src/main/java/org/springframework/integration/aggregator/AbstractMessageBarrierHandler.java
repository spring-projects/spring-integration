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

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.channel.MessageChannelTemplate;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.core.MessageHeaders;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.message.MessageHandler;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.integration.scheduling.IntervalTrigger;
import org.springframework.integration.scheduling.TaskScheduler;
import org.springframework.util.Assert;

/**
 * Base class for {@link MessageBarrier}-based Message Handlers. A
 * {@link MessageHandler} implementation that waits for a group of
 * {@link Message Messages} to arrive and processes them together. Uses a
 * {@link MessageBarrier} to store messages and to decide how the messages
 * should be released.
 * <p>
 * Each {@link Message} that is received by this handler will be associated
 * with a group based upon the '<code>correlationId</code>' property of its
 * header. If no such property is available, a {@link MessageHandlingException}
 * will be thrown.
 * <p>
 * The '<code>timeout</code>' value determines how long to wait for the complete
 * group after the arrival of the first {@link Message} of the group. The
 * default value is 1 minute. If the timeout elapses prior to completion, then
 * Messages with that timed-out 'correlationId' will be sent to the
 * 'discardChannel' if provided unless 'sendPartialResultsOnTimeout' is set to
 * true in which case the incomplete group will be sent to the output channel.
 * <p>
 * Subclasses must decide what kind of a Map they want to use, what is the logic
 * for adding messages to the barrier through the '<code>doAddMessage</code>'
 * method.
 * 
 * @author Mark Fisher
 * @author Marius Bogoevici
 */
public abstract class AbstractMessageBarrierHandler<T extends Map<K, Message<?>>, K>
		extends AbstractMessageHandler implements BeanFactoryAware, InitializingBean {

	public final static long DEFAULT_SEND_TIMEOUT = 1000;

	public final static long DEFAULT_TIMEOUT = 60000;

	public final static long DEFAULT_REAPER_INTERVAL = 1000;

	public final static int DEFAULT_TRACKED_CORRRELATION_ID_CAPACITY = 1000;

	protected final Log logger = LogFactory.getLog(this.getClass());

	private volatile MessageChannel outputChannel;

	private final MessageChannelTemplate channelTemplate = new MessageChannelTemplate();

	private volatile MessageChannel discardChannel;

	protected final ConcurrentMap<Object, MessageBarrier<T,K>> barriers = new ConcurrentHashMap<Object, MessageBarrier<T,K>>();

	private volatile long timeout = DEFAULT_TIMEOUT;

	private volatile boolean sendPartialResultOnTimeout = false;

	private volatile long reaperInterval = DEFAULT_REAPER_INTERVAL;

	private volatile int trackedCorrelationIdCapacity = DEFAULT_TRACKED_CORRRELATION_ID_CAPACITY;

	protected volatile BlockingQueue<Object> trackedCorrelationIds;

	private volatile boolean autoStartup = true;

	private volatile boolean initialized;

	private volatile TaskScheduler taskScheduler;

	private volatile ScheduledFuture<?> reaperFutureTask;


	public AbstractMessageBarrierHandler() {
		this.channelTemplate.setSendTimeout(DEFAULT_SEND_TIMEOUT);
	}


	public void setOutputChannel(MessageChannel outputChannel) {
		this.outputChannel = outputChannel;
	}

	/**
	 * Specify a channel for sending Messages that arrive after their
	 * aggregation group has either completed or timed-out.
	 */
	public void setDiscardChannel(MessageChannel discardChannel) {
		this.discardChannel = discardChannel;
	}

	/**
	 * Specify whether to aggregate and send the resulting Message when the
	 * timeout elapses prior to the CompletionStrategy returning true.
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
	 * Maximum time to wait (in milliseconds) for the completion strategy to
	 * become true. The default is 60000 (1 minute).
	 */
	public void setTimeout(long timeout) {
		Assert.isTrue(timeout >= 0, "'timeout' must not be negative");
		this.timeout = timeout;
	}

	public void setSendTimeout(long sendTimeout) {
		this.channelTemplate.setSendTimeout(sendTimeout);
	}

	public void setTaskScheduler(TaskScheduler taskScheduler) {
		Assert.notNull(taskScheduler, "taskScheduler must not be null");
		this.taskScheduler = taskScheduler;
	}

	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	public void setBeanFactory(BeanFactory beanFactory) {
		if (this.taskScheduler == null) {
			this.taskScheduler = IntegrationContextUtils.getRequiredTaskScheduler(beanFactory);
		}
	}

	public void afterPropertiesSet() {
		this.trackedCorrelationIds = new ArrayBlockingQueue<Object>(this.trackedCorrelationIdCapacity);
		if (this.autoStartup) {
			this.start();
		}
		this.initialized = true;
	}

	public boolean isRunning() {
		return this.reaperFutureTask != null;
	}

	public void start() {
		if (this.isRunning()) {
			return;
		}
		Assert.state(this.taskScheduler != null, "TaskScheduler must not be null");
		this.reaperFutureTask = this.taskScheduler.schedule(new PrunerTask(), new IntervalTrigger(this.reaperInterval,
				TimeUnit.MILLISECONDS));
	}

	public void stop() {
		if (this.isRunning()) {
			this.reaperFutureTask.cancel(true);
		}
	}

	@Override
	protected final void handleMessageInternal(Message<?> message) {
		if (!this.initialized) {
			this.afterPropertiesSet();
		}
		Object correlationId = message.getHeaders().getCorrelationId();
		if (correlationId == null) {
			throw new MessageHandlingException(message, this.getClass().getSimpleName()
					+ " requires the 'correlationId' property");
		}
		if (this.trackedCorrelationIds.contains(correlationId)) {
			if (logger.isDebugEnabled()) {
				logger.debug("Handling of Message group with correlationId '" + correlationId
						+ "' has already completed or timed out.");
			}
			this.discardMessage(message);
		}
		else {
			this.processMessage(message, correlationId);
		}
	}

	private void discardMessage(Message<?> message) {
		if (this.discardChannel != null) {
			boolean sent = this.channelTemplate.send(message, this.discardChannel);
			if (!sent && logger.isWarnEnabled()) {
				logger.warn("unable to send to 'discardChannel', message: " + message);
			}
		}
	}

	private void processMessage(Message<?> message, Object correlationId) {
		MessageBarrier<T,K> barrier = barriers.putIfAbsent(correlationId, createMessageBarrier());
		if (barrier == null) {
			barrier = barriers.get(message.getHeaders().getCorrelationId());
		}
		synchronized (barrier) {
			if (canAddMessage(message, barrier)) {
				doAddMessage(message, barrier);
			}
			processBarrier(barrier);
		}
	}

	protected final void sendReplies(Collection<Message<?>> messages, MessageChannel defaultReplyChannel) {
		if (messages.isEmpty()) {
			return;
		}
		for (Message<?> result : messages) {
			sendReply(result, defaultReplyChannel);
		}
	}

	protected final void sendReply(Message<?> message, MessageChannel defaultReplyChannel) {
		MessageChannel replyChannel = this.outputChannel;
		if (replyChannel == null) {
			replyChannel = this.resolveReplyChannelFromMessage(message);
			if (replyChannel == null) {
				replyChannel = defaultReplyChannel;
			}
		}
		if (replyChannel != null) {
			if (defaultReplyChannel != null && !defaultReplyChannel.equals(replyChannel)) {
				message = MessageBuilder.fromMessage(message)
						.setHeaderIfAbsent(MessageHeaders.REPLY_CHANNEL, defaultReplyChannel)
						.build();
			}
			this.channelTemplate.send(message, replyChannel);
		}
		else if (logger.isWarnEnabled()) {
			logger.warn("unable to determine reply target for aggregation result: " + message);
		}
	}

	protected final MessageChannel resolveReplyChannelFromMessage(Message<?> message) {
		Object replyChannel = message.getHeaders().getReplyChannel();
		if (replyChannel != null) {
			if (replyChannel instanceof MessageChannel) {
				return (MessageChannel) replyChannel;
			}
			if (logger.isWarnEnabled()) {
				logger.warn("Aggregator can only reply to a 'replyChannel' of type MessageChannel.");
			}
		}
		return null;
	}

	protected final void removeBarrier(Object correlationId) {
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

	/**
	 * Verifies that a message can be added to the barrier. To be overridden by subclasses, which may add 
	 * their own verifications. Subclasses overriding this method must call the method from the superclass.
	 */
	protected boolean canAddMessage(Message<?> message, MessageBarrier<T, K> barrier) {
		if (barrier.isComplete()) {
			if (logger.isDebugEnabled()) {
				logger.debug("Message received after aggregation has already completed: " + message);
			}
			return false;
		}
		return true;
	}
	

	/**
	 * Factory method for creating a MessageBarrier implementation.
	 */
	protected abstract MessageBarrier<T, K> createMessageBarrier();

	/**
	 * A method for processing the information in the message barrier after a message has been added or on pruning.
	 * The decision as to whether the messages from the {@link MessageBarrier}
	 * can be released normally belongs here, although calling code may forcibly set the MessageBarrier's 'complete'
	 * flag to true before invoking the method.
	 * @param barrier the {@link MessageBarrier} to be processed
	 */
	protected abstract void processBarrier(MessageBarrier<T, K> barrier);
	
	/**
	 * A method implemented by subclasses to add the incoming message to the message barrier. This is deferred to subclasses,
	 * as they should have full control over how the messages are indexed in the MessageBarrier.
	 */
	protected abstract void doAddMessage(Message<?> message, MessageBarrier<T, K> barrier);

	/**
	 * A task that runs periodically, pruning the timed-out message barriers.
	 */
	private class PrunerTask implements Runnable {

		public void run() {
			long currentTime = System.currentTimeMillis();
			for (Map.Entry<Object, MessageBarrier<T,K>> entry : barriers.entrySet()) {
				if (currentTime - entry.getValue().getTimestamp() >= timeout) {
					MessageBarrier<T,K> barrier = entry.getValue();
					synchronized (barrier) {
						removeBarrier(entry.getKey());
						if (sendPartialResultOnTimeout) {
							barrier.setComplete();
							processBarrier(barrier);
						}
						else {
							for (Object message : barrier.getMessages().values()) {
								if (logger.isDebugEnabled()) {
									logger.debug("Handling of Message group with correlationId '" + entry.getKey()
											+ "' has timed out.");
								}
								discardMessage((Message<?>) message);
							}
						}
					}
				}
			}
		}

	}

}
