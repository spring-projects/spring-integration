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

import java.util.Date;
import java.util.concurrent.CountDownLatch;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.integration.Message;
import org.springframework.integration.MessageHeaders;
import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.store.SimpleMessageStore;
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
public class DelayHandler extends AbstractReplyProducingMessageHandler implements DisposableBean {

	private final Log logger = LogFactory.getLog(this.getClass());

	private volatile String messageGroupId;

	private volatile long defaultDelay;

	private volatile String delayHeaderName;

	private boolean waitForTasksToCompleteOnShutdown = false;

	private volatile MessageGroupStore messageStore;

	private final CountDownLatch initializingLatch = new CountDownLatch(1);
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
		this.messageStore = messageStore;
	}

	/**
	 * Set whether to wait for scheduled tasks to complete on shutdown.
	 * <p>Default is "false". Switch this to "true" if you prefer
	 * fully completed tasks at the expense of a longer shutdown phase.
	 * <p/>
	 * This property will only have an effect for TaskScheduler implementations
	 * that extend from {@link ExecutorConfigurationSupport}.
	 *
	 * @see ExecutorConfigurationSupport#setWaitForTasksToCompleteOnShutdown(boolean)
	 */
	public void setWaitForTasksToCompleteOnShutdown(boolean waitForJobsToCompleteOnShutdown) {
		this.waitForTasksToCompleteOnShutdown = waitForJobsToCompleteOnShutdown;
	}

	@Override
	public String getComponentType() {
		return "delayer";
	}

	protected void onInit() {
		super.onInit();

		if (this.getTaskScheduler() instanceof ExecutorConfigurationSupport) {
			((ExecutorConfigurationSupport) this.getTaskScheduler()).setWaitForTasksToCompleteOnShutdown(this.waitForTasksToCompleteOnShutdown);
		}
		else if (logger.isWarnEnabled()) {
			logger.warn("The 'waitForJobsToCompleteOnShutdown' property is not supported for TaskScheduler of type [" +
					this.getTaskScheduler().getClass() + "]");
		}
		if (this.messageStore == null) {
			this.messageStore = new SimpleMessageStore();
		}

		this.reschedulePersistedMessagesOnStartup();
		this.initializingLatch.countDown();
	}

	@Override
	protected Object handleRequestMessage(Message<?> requestMessage) {
		try {
			this.initializingLatch.await();
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		boolean delayed = requestMessage instanceof DelayedMessageWrapper;

		if (!delayed) {
			long delay = this.determineDelayForMessage(requestMessage);
			if (delay > 0) {
				this.releaseMessageAfterDelay(requestMessage, delay);
				return null;
			}
		}

		// no delay
		return requestMessage.getPayload();
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
		Assert.state(this.messageStore != null, "MessageStore must not be null");
		this.messageStore.addMessageToGroup(this.messageGroupId, message);
		this.getTaskScheduler().schedule(new Runnable() {
			public void run() {
				releaseMessage(message);
			}
		}, new Date(System.currentTimeMillis() + delay));
	}

	private void releaseMessage(Message<?> message) {
		Assert.state(this.messageStore != null, "MessageStore must not be null");
		MessageGroup messageGroup = this.messageStore.removeMessageFromGroup(messageGroupId, message);
		//	TODO add 'notNull' check for a complete removal message during current transaction
		this.handleMessageInternal(new DelayedMessageWrapper(message));
	}

	/**
	 * Used for reading persisted Messages in the 'messageStore' to reschedule them upon application restart.
	 * The logic is based on iteration over 'messageGroupSize' abd uses {@link MessageGroupStore#pollMessageFromGroup(Object)}
	 * to retrieve Messages one by one and invokes <code>this.releaseMessageAfterDelay(message, delay)</code>
	 * independently of value of message's 'delay'.
	 * This behavior is dictated by the avoidance of overhead on the initializing phase.
	 */
	private void reschedulePersistedMessagesOnStartup() {
		Assert.state(this.messageStore != null, "MessageStore must not be null");
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

	public void destroy() throws Exception {
		if (this.getTaskScheduler() instanceof DisposableBean) {
			((DisposableBean) this.getTaskScheduler()).destroy();
		}
	}

	private class DelayedMessageWrapper implements Message<Object> {

		private final Message<?> original;

		public DelayedMessageWrapper(Message<?> original){
			this.original = original;
		}

		public MessageHeaders getHeaders() {
			return this.original.getHeaders();
		}

		public Object getPayload() {
			return original.getPayload();
		}
	}

}
