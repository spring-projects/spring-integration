/*
 * Copyright 2002-2013 the original author or authors.
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
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.aopalliance.aop.Advice;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.integration.Message;
import org.springframework.integration.MessagingException;
import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.store.MessageStore;
import org.springframework.integration.store.SimpleMessageStore;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;
import org.springframework.util.CollectionUtils;

/**
 * A {@link MessageHandler} that is capable of delaying the continuation of a
 * Message flow based on the presence of a delay header on an inbound Message
 * or a default delay value configured on this handler. Note that the
 * continuation of the flow is delegated to a {@link TaskScheduler}, and
 * therefore, the calling thread does not block. The advantage of this approach
 * is that many delays can be managed concurrently, even very long delays,
 * without producing a buildup of blocked Threads.
 * <p>
 * One thing to keep in mind, however, is that any active transactional context
 * will not propagate from the original sender to the eventual recipient. This
 * is a side-effect of passing the Message to the output channel after the
 * delay with a different Thread in control.
 * <p>
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

@ManagedResource
public class DelayHandler extends AbstractReplyProducingMessageHandler implements DelayHandlerManagement, ApplicationListener<ContextRefreshedEvent> {

	private final String messageGroupId;

	private volatile long defaultDelay;

	private volatile String delayHeaderName;

	private volatile MessageGroupStore messageStore;

	private volatile List<Advice> delayedAdviceChain;

	private final AtomicBoolean initialized = new AtomicBoolean();

	private volatile MessageHandler releaseHandler = new ReleaseMessageHandler();

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
	 * a delay should <em>only</em> be applied to Messages with a
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

	/**
	 * Specify the {@code List<Advice>} to advise {@link DelayHandler.ReleaseMessageHandler} proxy.
	 * Usually used to add transactions to delayed messages retrieved from a transactional message store.
	 *
	 * @see #createReleaseMessageTask
	 */
	public void setDelayedAdviceChain(List<Advice> delayedAdviceChain) {
		Assert.notNull(delayedAdviceChain, "delayedAdviceChain must not be null");
		this.delayedAdviceChain = delayedAdviceChain;
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
		else {
			Assert.isInstanceOf(MessageStore.class, this.messageStore);
		}

		this.releaseHandler = this.createReleaseMessageTask();
	}

	private MessageHandler createReleaseMessageTask() {
		ReleaseMessageHandler releaseHandler = new ReleaseMessageHandler();

		if (!CollectionUtils.isEmpty(this.delayedAdviceChain)) {
			ProxyFactory proxyFactory = new ProxyFactory(releaseHandler);
			for (Advice advice : delayedAdviceChain) {
				proxyFactory.addAdvice(advice);
			}
			return (MessageHandler) proxyFactory.getProxy(ClassUtils.getDefaultClassLoader());
		}
		return releaseHandler;
	}

	/**
	 * Checks if 'requestMessage' wasn't delayed before
	 * ({@link #releaseMessageAfterDelay} and {@link DelayHandler.DelayedMessageWrapper}).
	 * Than determine 'delay' for 'requestMessage' ({@link #determineDelayForMessage})
	 * and if {@code delay > 0} schedules 'releaseMessage' task after 'delay'.
	 *
	 * @param requestMessage - the Message which may be delayed.
	 * @return - <code>null</code> if 'requestMessage' is delayed,
	 *         otherwise - 'payload' from 'requestMessage'.
	 *
	 * @see #releaseMessage
	 */

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
		Message<?> delayedMessage = message;

		DelayedMessageWrapper messageWrapper = null;
		if (message.getPayload() instanceof DelayedMessageWrapper) {
			messageWrapper = (DelayedMessageWrapper) message.getPayload();
		}
		else {
			messageWrapper = new DelayedMessageWrapper(message);
			delayedMessage = MessageBuilder.withPayload(messageWrapper).copyHeaders(message.getHeaders()).build();
			this.messageStore.addMessageToGroup(this.messageGroupId, delayedMessage);
		}

		final Message<?> messageToSchedule = delayedMessage;

		this.getTaskScheduler().schedule(new Runnable() {
			public void run() {
				releaseMessage(messageToSchedule);
			}
		}, new Date(messageWrapper.getRequestDate() + delay));
	}

	private void releaseMessage(Message<?> message) {
		this.releaseHandler.handleMessage(message);
	}

	private void doReleaseMessage(Message<?> message) {
		if (this.messageStore instanceof SimpleMessageStore
				|| ((MessageStore) this.messageStore).removeMessage(message.getHeaders().getId()) != null) {
			this.messageStore.removeMessageFromGroup(this.messageGroupId, message);
			this.handleMessageInternal(message);
		}
		else {
			if (logger.isDebugEnabled()) {
				logger.debug("No message in the Message Store to release: " + message +
						". Likely another instance has already released it.");
			}
		}
	}

	public int getDelayedMessageCount() {
		return this.messageStore.messageGroupSize(this.messageGroupId);
	}

	/**
	 * Used for reading persisted Messages in the 'messageStore'
	 * to reschedule them e.g. upon application restart.
	 * The logic is based on iteration over 'messageGroup.getMessages()'
	 * and schedules task about 'delay' logic.
	 * This behavior is dictated by the avoidance of invocation thread overload.
	 */
	public void reschedulePersistedMessages() {
		MessageGroup messageGroup = this.messageStore.getMessageGroup(this.messageGroupId);
		for (final Message<?> message : messageGroup.getMessages()) {
			this.getTaskScheduler().schedule(new Runnable() {
				public void run() {
					long delay = determineDelayForMessage(message);
					if (delay > 0) {
						releaseMessageAfterDelay(message, delay);
					}
					else {
						releaseMessage(message);
					}
				}
			}, new Date());
		}
	}

	/**
	 * Handles {@link ContextRefreshedEvent} to invoke {@link #reschedulePersistedMessages}
	 * as late as possible after application context startup.
	 * Also it checks {@link #initialized} to ignore
	 * other {@link ContextRefreshedEvent}s which may be published
	 * in the 'parent-child' contexts, e.g. in the Spring-MVC applications.
	 *
	 * @param event - {@link ContextRefreshedEvent} which occurs
	 *              after Application context is completely initialized.
	 *
	 * @see #reschedulePersistedMessages
	 */
	public void onApplicationEvent(ContextRefreshedEvent event) {
		if (!this.initialized.getAndSet(true)) {
			this.reschedulePersistedMessages();
		}
	}


	/**
	 * Delegate {@link MessageHandler} implementation for 'release Message task'.
	 * Used as 'pointcut' to wrap 'release Message task' with <code>adviceChain</code>.
	 *
	 * @see @createReleaseMessageTask
	 * @see @releaseMessage
	 */
	private class ReleaseMessageHandler implements MessageHandler {

		public void handleMessage(Message<?> message) throws MessagingException {
			DelayHandler.this.doReleaseMessage(message);
		}

	}


	private static final class DelayedMessageWrapper implements Serializable {

		private static final long serialVersionUID = -4739802369074947045L;

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

		@Override
		public boolean equals(Object o) {
			if (this == o) return true;
			if (o == null || getClass() != o.getClass()) return false;

			DelayedMessageWrapper that = (DelayedMessageWrapper) o;

			return this.original.equals(that.original);
		}

		@Override
		public int hashCode() {
			return this.original.hashCode();
		}

	}

}
