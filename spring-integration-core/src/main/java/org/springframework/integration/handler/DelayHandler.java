/*
 * Copyright 2002-2012 the original author or authors.
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

import java.io.Serializable;
import java.util.Date;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.SmartLifecycle;
import org.springframework.integration.Message;
import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.store.SimpleMessageStore;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ExecutorConfigurationSupport;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.Assert;

/**
 * A {@link MessageHandler} that is capable of delaying the continuation of a
 * Message flow based on the presence of a delay header on an inbound Message
 * or a default delay value configured on this handler. Note that the
 * continuation of the flow is delegated to a {@link TaskScheduler}, and
 * therefore, the calling thread does not block. The advantage of this approach
 * is that many delays can be managed concurrently, even very long delays,
 * without producing a buildup of blocked Threads.
 * <p/>
 * One thing to keep in mind, however, is that any active transactional context
 * will not propagate from the original sender to the eventual recipient. This
 * is a side-effect of passing the Message to the output channel after the
 * delay with a different Thread in control.
 * <p/>
 * When this handler's 'delayHeaderName' property is configured, that value, if
 * present on a Message, will take precedence over the handler's 'defaultDelay'
 * value. The actual header value may be a long, a String that can be parsed
 * as a long, or a Date. If it is a long, it will be interpreted as the length
 * of time to delay in milliseconds counting from the current time (e.g. a
 * value of 5000 indicates that the Message can be released as soon as five
 * seconds from the current time). If the value is a Date, it will be
 * delayed at least until that Date occurs (i.e. the delay in that case is
 * equivalent to <code>headerDate.getTime() - new Date().getTime()</code>).
 *
 * @author Mark Fisher
 * @author Artem Bilan
 * @since 1.0.3
 */
public class DelayHandler extends AbstractReplyProducingMessageHandler implements SmartLifecycle, DisposableBean {

	private volatile String messageGroupId;

	private volatile long defaultDelay;

	private volatile String delayHeaderName;

	private volatile MessageGroupStore messageStore;

	private boolean taskSchedulerProvided;

	private volatile boolean autoStartup = true;

	private volatile int phase = Integer.MAX_VALUE;

	private volatile boolean running;

	private final ReentrantLock lifecycleLock = new ReentrantLock();

	/**
	 * Create a DelayHandler with the given 'messageGroupId' that is used as 'key' for {@link MessageGroup}
	 * to store delayed Messages in the {@link MessageGroupStore}. The sending of Messages after
	 * the delay will be handled by registered in the ApplicationContext default {@link ThreadPoolTaskScheduler}.
	 *
	 * @see IntegrationObjectSupport#getTaskScheduler()
	 */
	public DelayHandler(String messageGroupId) {
		Assert.notNull(messageGroupId, "'messageGroupId' must not be null");
		this.messageGroupId = messageGroupId;
	}

	/**
	 * Create a DelayHandler with the given default delay. The sending of Messages
	 * after the delay will be handled by the provided {@link TaskScheduler}.
	 */
	public DelayHandler(String messageGroupId, TaskScheduler taskScheduler) {
		this(messageGroupId);
		this.setTaskScheduler(taskScheduler);
	}


	/**
	 * Set the default delay in milliseconds. If no 'delayHeaderName' property
	 * has been provided, the default delay will be applied to all Messages. If
	 * a delay should <emphasis>only</emphasis> be applied to Messages with a
	 * header, then set this value to 0.
	 */
	public void setDefaultDelay(long defaultDelay) {
		this.defaultDelay = defaultDelay;
	}

	/**
	 * Specify the name of the header that should be checked for a delay period
	 * (in milliseconds) or a Date to delay until. If this property is set, any
	 * such header value will take precedence over this handler's default delay.
	 */
	public void setDelayHeaderName(String delayHeaderName) {
		this.delayHeaderName = delayHeaderName;
	}

	/**
	 * Specify the {@link MessageGroupStore} that should be used to store Messages
	 * while awaiting the delay.
	 */
	public void setMessageStore(MessageGroupStore messageStore) {
		Assert.state(messageStore != null, "MessageStore must not be null");
		this.messageStore = messageStore;
	}

	@Override
	public void setTaskScheduler(TaskScheduler taskScheduler) {
		super.setTaskScheduler(taskScheduler);
		this.taskSchedulerProvided = true;
	}

	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	public void setPhase(int phase) {
		this.phase = phase;
	}

	@Override
	public String getComponentType() {
		return "delayer";
	}

	@Override
	protected void onInit() {
		super.onInit();
		if (this.messageStore == null) {
			this.messageStore = new SimpleMessageStore();
		}
	}

	@Override
	protected Object handleRequestMessage(Message<?> requestMessage) {

		boolean delayed = requestMessage.getPayload() instanceof DelayedMessageWrapper;

		if (!delayed) {
			long delay = this.determineDelayForMessage(requestMessage);
			if (delay > 0) {
				this.releaseMessageAfterDelay(requestMessage, delay);
				return null;
			}
		}

		// no delay
		Object payload = requestMessage.getPayload();
		return delayed ? ((DelayedMessageWrapper) payload).getOriginal().getPayload() : payload;
	}

	private long determineDelayForMessage(Message<?> message) {
		long delay = this.defaultDelay;
		if (this.delayHeaderName != null) {
			Object headerValue = message.getHeaders().get(this.delayHeaderName);
			if (headerValue instanceof Date) {
				delay = ((Date) headerValue).getTime() - new Date().getTime();
			}
			else if (headerValue != null) {
				try {
					delay = Long.valueOf(headerValue.toString());
				}
				catch (NumberFormatException e) {
					if (logger.isDebugEnabled()) {
						logger.debug("Failed to parse delay from header value '" + headerValue.toString() +
								"', will fall back to default delay: " + this.defaultDelay);
					}
				}
			}
		}
		return delay;
	}

	private void releaseMessageAfterDelay(final Message<?> message, long delay) {

		DelayedMessageWrapper messageWrapper = null;
		if (message.getPayload() instanceof DelayedMessageWrapper) {
			messageWrapper = (DelayedMessageWrapper) message.getPayload();
		}
		else {
			messageWrapper = new DelayedMessageWrapper(message);
		}
		final Message messageToSchedule = MessageBuilder.withPayload(messageWrapper).copyHeaders(message.getHeaders()).build();
		this.messageStore.addMessageToGroup(this.messageGroupId, messageToSchedule);

		this.lifecycleLock.lock();
		try {
			if (!this.running) {
				if (this.logger.isWarnEnabled()) {
					this.logger.warn(this + " isn't running. " +
							"It can't 'schedule' messages for delay. 'SmartLifecycle#start()' initiates " +
							"rescheduling process for persisted messages in the MessageStore.");
				}
				return;
			}
		}
		finally {
			this.lifecycleLock.unlock();
		}

		this.getTaskScheduler().schedule(new Runnable() {
			public void run() {
				releaseMessage(messageToSchedule);
			}
		}, new Date(messageWrapper.getRequestDate() + delay));
	}

	private void releaseMessage(Message<?> message) {
		this.messageStore.removeMessageFromGroup(messageGroupId, message);
		this.handleMessageInternal(message);
	}

	/**
	 * Used for reading persisted Messages in the 'messageStore' to reschedule them upon application restart.
	 * The logic is based on iteration over 'messageGroupSize' abd uses {@link MessageGroupStore#pollMessageFromGroup(Object)}
	 * to retrieve Messages one by one and invokes <code>this.releaseMessageAfterDelay(message, delay)</code>
	 * independently of value of message's 'delay'.
	 * This behavior is dictated by the avoidance of overhead on the initializing phase.
	 */
	private void reschedulePersistedMessagesOnStartup() {
		int messageGroupSize = this.messageStore.messageGroupSize(this.messageGroupId);
		while (messageGroupSize > 0) {
			Message<?> message = this.messageStore.pollMessageFromGroup(messageGroupId);
			if (message != null) {
				long delay = this.determineDelayForMessage(message);
				this.releaseMessageAfterDelay(message, delay);
			}
			messageGroupSize--;
		}
	}

	// SmartLifecycle implementation

	@Override
	public final boolean isAutoStartup() {
		return this.autoStartup;
	}

	@Override
	public final int getPhase() {
		return this.phase;
	}

	@Override
	public final boolean isRunning() {
		this.lifecycleLock.lock();
		try {
			return this.running;
		}
		finally {
			this.lifecycleLock.unlock();
		}
	}

	@Override
	public final void start() {
		this.lifecycleLock.lock();
		try {
			if (!this.running) {
				this.running = true;
				this.doStart();
				if (logger.isInfoEnabled()) {
					logger.info("started " + this);
				}
			}
		}
		finally {
			this.lifecycleLock.unlock();
		}
	}

	@Override
	public final void stop() {
		this.lifecycleLock.lock();
		try {
			if (this.running) {
				this.doStop();
				this.running = false;
				if (logger.isInfoEnabled()) {
					logger.info("stopped " + this);
				}
			}
		}
		finally {
			this.lifecycleLock.unlock();
		}
	}

	@Override
	public final void stop(Runnable callback) {
		this.lifecycleLock.lock();
		try {
			this.stop();
			callback.run();
		}
		finally {
			this.lifecycleLock.unlock();
		}
	}

	private void doStart() {
		if (this.taskSchedulerProvided) {
			if (this.getTaskScheduler() instanceof ExecutorConfigurationSupport) {
				((ExecutorConfigurationSupport) this.getTaskScheduler()).initialize();
			}
		}
		else {
			this.logger.warn("'SmartLifecycle#start()' is prevented when delayer uses global shared TaskScheduler.");
		}
		this.reschedulePersistedMessagesOnStartup();
	}

	private void doStop() {
	  if (this.taskSchedulerProvided) {
		  if (this.getTaskScheduler() instanceof ExecutorConfigurationSupport) {
			  ((ExecutorConfigurationSupport) this.getTaskScheduler()).destroy();
		  }
	  }
		else {
		  this.logger.warn("'SmartLifecycle#stop()' is prevented when delayer uses global shared TaskScheduler.");
	  }
	}

	@Override
	public void destroy() throws Exception {
		this.stop();
	}

	private static final class DelayedMessageWrapper implements Serializable {

		private final long requestDate = System.currentTimeMillis();

		private final Message<?> original;

		public DelayedMessageWrapper(Message<?> original) {
			this.original = original;
		}

		public long getRequestDate() {
			return this.requestDate;
		}

		public Message<?> getOriginal() {
			return this.original;
		}
	}

}
