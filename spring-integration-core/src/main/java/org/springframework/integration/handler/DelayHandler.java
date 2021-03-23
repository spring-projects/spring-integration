/*
 * Copyright 2002-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.handler;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

import org.aopalliance.aop.Advice;

import org.springframework.aop.framework.ProxyFactory;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.EvaluationException;
import org.springframework.expression.Expression;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.IntegrationPatternType;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.store.MessageStore;
import org.springframework.integration.store.SimpleMessageStore;
import org.springframework.integration.support.management.IntegrationManagedResource;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.core.DestinationResolver;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

/**
 * A {@link MessageHandler} that is capable of delaying the continuation of a Message flow
 * based on the result of evaluation {@code delayExpression} on an inbound {@link Message}
 * or a default delay value configured on this handler. Note that the continuation of the
 * flow is delegated to a {@link TaskScheduler}, and therefore, the calling thread does
 * not block. The advantage of this approach is that many delays can be managed
 * concurrently, even very long delays, without producing a buildup of blocked Threads.
 * <p>
 * One thing to keep in mind, however, is that any active transactional context will not
 * propagate from the original sender to the eventual recipient. This is a side-effect of
 * passing the Message to the output channel after the delay with a different Thread in
 * control.
 * <p>
 * When this handler's {@code delayExpression} property is configured, that evaluation
 * result value will take precedence over the handler's {@code defaultDelay} value. The
 * actual evaluation result value may be a long, a String that can be parsed as a long, or
 * a Date. If it is a long, it will be interpreted as the length of time to delay in
 * milliseconds counting from the current time (e.g. a value of 5000 indicates that the
 * Message can be released as soon as five seconds from the current time). If the value is
 * a Date, it will be delayed at least until that Date occurs (i.e. the delay in that case
 * is equivalent to {@code headerDate.getTime() - new Date().getTime()}).
 *
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 1.0.3
 */

@ManagedResource
@IntegrationManagedResource
public class DelayHandler extends AbstractReplyProducingMessageHandler implements DelayHandlerManagement,
		ApplicationListener<ContextRefreshedEvent> {

	public static final int DEFAULT_MAX_ATTEMPTS = 5;

	public static final long DEFAULT_RETRY_DELAY = 1_000;

	private final String messageGroupId;

	private final ConcurrentMap<String, AtomicInteger> deliveries = new ConcurrentHashMap<>();

	private long defaultDelay;

	private Expression delayExpression;

	private boolean ignoreExpressionFailures = true;

	private MessageGroupStore messageStore;

	private List<Advice> delayedAdviceChain;

	private final AtomicBoolean initialized = new AtomicBoolean();

	private MessageHandler releaseHandler = new ReleaseMessageHandler();

	private EvaluationContext evaluationContext;

	private MessageChannel delayedMessageErrorChannel;

	private String delayedMessageErrorChannelName;

	private int maxAttempts = DEFAULT_MAX_ATTEMPTS;

	private long retryDelay = DEFAULT_RETRY_DELAY;

	/**
	 * Create a DelayHandler with the given 'messageGroupId' that is used as 'key' for
	 * {@link MessageGroup} to store delayed Messages in the {@link MessageGroupStore}.
	 * The sending of Messages after the delay will be handled by registered in the
	 * ApplicationContext default
	 * {@link org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler}.
	 * @param messageGroupId The message group identifier.
	 * @see #getTaskScheduler()
	 */
	public DelayHandler(String messageGroupId) {
		Assert.notNull(messageGroupId, "'messageGroupId' must not be null");
		this.messageGroupId = messageGroupId;
	}

	/**
	 * Create a DelayHandler with the given default delay. The sending of Messages after
	 * the delay will be handled by the provided {@link TaskScheduler}.
	 * @param messageGroupId The message group identifier.
	 * @param taskScheduler A task scheduler.
	 */
	public DelayHandler(String messageGroupId, TaskScheduler taskScheduler) {
		this(messageGroupId);
		setTaskScheduler(taskScheduler);
	}

	/**
	 * Set the default delay in milliseconds. If no {@code delayExpression} property has
	 * been provided, the default delay will be applied to all Messages. If a delay should
	 * <em>only</em> be applied to Messages with evaluation result from
	 * {@code delayExpression}, then set this value to 0.
	 * @param defaultDelay The default delay in milliseconds.
	 */
	public void setDefaultDelay(long defaultDelay) {
		this.defaultDelay = defaultDelay;
	}

	/**
	 * Specify the {@link Expression} that should be checked for a delay period (in
	 * milliseconds) or a Date to delay until. If this property is set, the result of the
	 * expression evaluation (if not null) will take precedence over this handler's
	 * default delay.
	 * @param delayExpression The delay expression.
	 */
	public void setDelayExpression(Expression delayExpression) {
		this.delayExpression = delayExpression;
	}

	/**
	 * Specify the {@code Expression} that should be checked for a delay period (in
	 * milliseconds) or a Date to delay until. If this property is set, the result of the
	 * expression evaluation (if not null) will take precedence over this handler's
	 * default delay.
	 * @param delayExpression The delay expression.
	 * @since 5.0
	 */
	public void setDelayExpressionString(String delayExpression) {
		this.delayExpression = EXPRESSION_PARSER.parseExpression(delayExpression);
	}

	/**
	 * Specify whether {@code Exceptions} thrown by {@link #delayExpression} evaluation
	 * should be ignored (only logged). In this case case the delayer will fall back to
	 * the to the {@link #defaultDelay}. If this property is specified as {@code false},
	 * any {@link #delayExpression} evaluation {@code Exception} will be thrown to the
	 * caller without falling back to the to the {@link #defaultDelay}. Default is
	 * {@code true}.
	 * @param ignoreExpressionFailures true if expression evaluation failures should be
	 * ignored.
	 * @see #determineDelayForMessage
	 */
	public void setIgnoreExpressionFailures(boolean ignoreExpressionFailures) {
		this.ignoreExpressionFailures = ignoreExpressionFailures;
	}

	/**
	 * Specify the {@link MessageGroupStore} that should be used to store Messages while
	 * awaiting the delay.
	 * @param messageStore The message store.
	 */
	public void setMessageStore(MessageGroupStore messageStore) {
		Assert.state(messageStore != null, "MessageStore must not be null");
		this.messageStore = messageStore;
	}

	/**
	 * Specify the {@code List<Advice>} to advise
	 * {@link DelayHandler.ReleaseMessageHandler} proxy. Usually used to add transactions
	 * to delayed messages retrieved from a transactional message store.
	 * @param delayedAdviceChain The advice chain.
	 * @see #createReleaseMessageTask
	 */
	public void setDelayedAdviceChain(List<Advice> delayedAdviceChain) {
		Assert.notNull(delayedAdviceChain, "delayedAdviceChain must not be null");
		this.delayedAdviceChain = delayedAdviceChain;
	}

	/**
	 * Set a message channel to which an {@link ErrorMessage} will be sent if sending the
	 * released message fails. If the error flow returns normally, the release is
	 * complete. If the error flow throws an exception, the release will be re-attempted.
	 * If there is a transaction advice on the release task, the error flow is called
	 * within the transaction.
	 * @param delayedMessageErrorChannel the channel.
	 * @see #setMaxAttempts(int)
	 * @see #setRetryDelay(long)
	 * @since 5.0.8
	 */
	public void setDelayedMessageErrorChannel(MessageChannel delayedMessageErrorChannel) {
		this.delayedMessageErrorChannel = delayedMessageErrorChannel;
	}

	/**
	 * Set a message channel name to which an {@link ErrorMessage} will be sent if sending
	 * the released message fails. If the error flow returns normally, the release is
	 * complete. If the error flow throws an exception, the release will be re-attempted.
	 * If there is a transaction advice on the release task, the error flow is called
	 * within the transaction.
	 * @param delayedMessageErrorChannelName the channel name.
	 * @see #setMaxAttempts(int)
	 * @see #setRetryDelay(long)
	 * @since 5.0.8
	 */
	public void setDelayedMessageErrorChannelName(String delayedMessageErrorChannelName) {
		this.delayedMessageErrorChannelName = delayedMessageErrorChannelName;
	}

	/**
	 * Set the maximum number of release attempts for when message release fails. Default
	 * {@value #DEFAULT_MAX_ATTEMPTS}.
	 * @param maxAttempts the max attempts.
	 * @see #setRetryDelay(long)
	 * @since 5.0.8
	 */
	public void setMaxAttempts(int maxAttempts) {
		this.maxAttempts = maxAttempts;
	}

	/**
	 * Set an additional delay to apply when retrying after a release failure. Default
	 * {@value #DEFAULT_RETRY_DELAY}.
	 * @param retryDelay the retry delay.
	 * @see #setMaxAttempts(int)
	 * @since 5.0.8
	 */
	public void setRetryDelay(long retryDelay) {
		this.retryDelay = retryDelay;
	}

	private MessageChannel getErrorChannel() {
		if (this.delayedMessageErrorChannel != null) {
			return this.delayedMessageErrorChannel;
		}
		DestinationResolver<MessageChannel> channelResolver = getChannelResolver();
		if (this.delayedMessageErrorChannelName != null && channelResolver != null) {
			this.delayedMessageErrorChannel = channelResolver.resolveDestination(this.delayedMessageErrorChannelName);
		}
		return this.delayedMessageErrorChannel;
	}

	@Override
	public String getComponentType() {
		return "delayer";
	}

	@Override
	public IntegrationPatternType getIntegrationPatternType() {
		return IntegrationPatternType.delayer;
	}

	@Override
	protected void doInit() {
		if (this.messageStore == null) {
			this.messageStore = new SimpleMessageStore();
		}
		else {
			Assert.isInstanceOf(MessageStore.class, this.messageStore);
		}
		this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(getBeanFactory());
		this.releaseHandler = createReleaseMessageTask();
	}

	private MessageHandler createReleaseMessageTask() {
		ReleaseMessageHandler handler = new ReleaseMessageHandler();

		if (!CollectionUtils.isEmpty(this.delayedAdviceChain)) {
			ProxyFactory proxyFactory = new ProxyFactory(handler);
			for (Advice advice : this.delayedAdviceChain) {
				proxyFactory.addAdvice(advice);
			}
			return (MessageHandler) proxyFactory.getProxy(getApplicationContext().getClassLoader());
		}
		return handler;
	}

	@Override
	protected boolean shouldCopyRequestHeaders() {
		return false;
	}

	/**
	 * Check if 'requestMessage' wasn't delayed before ({@link #releaseMessageAfterDelay}
	 * and {@link DelayHandler.DelayedMessageWrapper}). Than determine 'delay' for
	 * 'requestMessage' ({@link #determineDelayForMessage}) and if {@code delay > 0}
	 * schedules 'releaseMessage' task after 'delay'.
	 * @param requestMessage - the Message which may be delayed.
	 * @return - {@code null} if 'requestMessage' is delayed, otherwise - 'payload' from
	 * 'requestMessage'.
	 * @see #releaseMessage
	 */
	@Override
	protected Object handleRequestMessage(Message<?> requestMessage) {
		boolean delayed = requestMessage.getPayload() instanceof DelayedMessageWrapper;

		if (!delayed) {
			long delay = determineDelayForMessage(requestMessage);
			if (delay > 0) {
				releaseMessageAfterDelay(requestMessage, delay);
				return null;
			}
		}

		// no delay
		return delayed
				? ((DelayedMessageWrapper) requestMessage.getPayload()).getOriginal()
				: requestMessage;
	}

	private long determineDelayForMessage(Message<?> message) {
		if (this.delayExpression != null) {
			return determineDelayFromExpression(message);
		}
		else {
			return this.defaultDelay;
		}
	}

	private long determineDelayFromExpression(Message<?> message) {
		long delay = this.defaultDelay;
		DelayedMessageWrapper delayedMessageWrapper = null;
		if (message.getPayload() instanceof DelayedMessageWrapper) {
			delayedMessageWrapper = (DelayedMessageWrapper) message.getPayload();
		}
		Exception delayValueException = null;
		Object delayValue = null;
		try {
			delayValue = this.delayExpression.getValue(this.evaluationContext,
					delayedMessageWrapper != null
							? delayedMessageWrapper.getOriginal()
							: message);
		}
		catch (EvaluationException e) {
			delayValueException = e;
		}
		if (delayValue instanceof Date) {
			long current =
					delayedMessageWrapper != null
							? delayedMessageWrapper.getRequestDate()
							: System.currentTimeMillis();
			delay = ((Date) delayValue).getTime() - current;
		}
		else if (delayValue != null) {
			try {
				delay = Long.parseLong(delayValue.toString());
			}
			catch (NumberFormatException e) {
				delayValueException = e;
			}
		}
		if (delayValueException != null) {
			handleDelayValueException(delayValueException);
		}
		return delay;
	}

	private void handleDelayValueException(Exception delayValueException) {
		if (this.ignoreExpressionFailures) {
			logger.debug(() -> "Failed to get delay value from 'delayExpression': " +
					delayValueException.getMessage() +
					". Will fall back to default delay: " + this.defaultDelay);
		}
		else {
			throw new IllegalStateException("Error occurred during 'delay' value determination", delayValueException);
		}
	}

	private void releaseMessageAfterDelay(final Message<?> message, long delay) {
		Message<?> delayedMessage = message;

		DelayedMessageWrapper messageWrapper;
		if (message.getPayload() instanceof DelayedMessageWrapper) {
			messageWrapper = (DelayedMessageWrapper) message.getPayload();
		}
		else {
			messageWrapper = new DelayedMessageWrapper(message, System.currentTimeMillis());
			delayedMessage = getMessageBuilderFactory()
					.withPayload(messageWrapper)
					.copyHeaders(message.getHeaders())
					.build();
			this.messageStore.addMessageToGroup(this.messageGroupId, delayedMessage);
		}

		Runnable releaseTask;

		if (this.messageStore instanceof SimpleMessageStore) {
			final Message<?> messageToSchedule = delayedMessage;

			releaseTask = () -> releaseMessage(messageToSchedule);
		}
		else {
			final UUID messageId = delayedMessage.getHeaders().getId();

			releaseTask = () -> {
				Message<?> messageToRelease = getMessageById(messageId);
				if (messageToRelease != null) {
					releaseMessage(messageToRelease);
				}
			};
		}

		Date startTime = new Date(messageWrapper.getRequestDate() + delay);

		if (TransactionSynchronizationManager.isSynchronizationActive() &&
				TransactionSynchronizationManager.isActualTransactionActive()) {

			TransactionSynchronizationManager.registerSynchronization(
					new TransactionSynchronization() {

						@Override
						public void afterCommit() {
							getTaskScheduler().schedule(releaseTask, startTime);
						}

					});
		}
		else {
			getTaskScheduler().schedule(releaseTask, startTime);
		}
	}

	private Message<?> getMessageById(UUID messageId) {
		Message<?> theMessage = ((MessageStore) this.messageStore).getMessage(messageId);

		if (theMessage == null) {
			logger.debug(() -> "No message in the Message Store for id: " + messageId +
					". Likely another instance has already released it.");
			return null;
		}
		else {
			return theMessage;
		}
	}

	private void releaseMessage(Message<?> message) {
		String identity = ObjectUtils.getIdentityHexString(message);
		this.deliveries.putIfAbsent(identity, new AtomicInteger());
		try {
			this.releaseHandler.handleMessage(message);
			this.deliveries.remove(identity);
		}
		catch (Exception e) {
			if (getErrorChannel() != null) {
				ErrorMessage errorMessage = new ErrorMessage(e,
						Collections.singletonMap(IntegrationMessageHeaderAccessor.DELIVERY_ATTEMPT,
								new AtomicInteger(this.deliveries.get(identity).get() + 1)),
						message);
				try {
					if (!(getErrorChannel().send(errorMessage))) {
						this.logger.debug(() -> "Failed to send error message: " + errorMessage);
						rescheduleForRetry(message, identity);
					}
					else {
						this.deliveries.remove(identity);
					}
				}
				catch (Exception e1) {
					logger.debug(e1, () -> "Error flow threw an exception for message: " + message);
					rescheduleForRetry(message, identity);
				}
			}
			else {
				logger.debug(e, () -> "Release flow threw an exception for message: " + message);
				if (!rescheduleForRetry(message, identity)) {
					throw e; // there might be an error handler on the scheduler
				}
			}
		}
	}

	private boolean rescheduleForRetry(Message<?> message, String identity) {
		if (this.deliveries.get(identity).incrementAndGet() >= this.maxAttempts) {
			this.logger.error(() -> "Discarding; maximum release attempts reached for: " + message);
			this.deliveries.remove(identity);
			return false;
		}
		if (this.retryDelay <= 0) {
			rescheduleNow(message);
		}
		else {
			rescheduleAt(message, new Date(System.currentTimeMillis() + this.retryDelay));
		}
		return true;
	}

	private void rescheduleNow(final Message<?> message) {
		rescheduleAt(message, new Date());
	}

	protected void rescheduleAt(final Message<?> message, Date startTime) {
		getTaskScheduler()
				.schedule(() -> releaseMessage(message), startTime);
	}

	private void doReleaseMessage(Message<?> message) {
		if (removeDelayedMessageFromMessageStore(message)
				|| this.deliveries.get(ObjectUtils.getIdentityHexString(message)).get() > 0) {
			if (!(this.messageStore instanceof SimpleMessageStore)) {
				this.messageStore.removeMessagesFromGroup(this.messageGroupId, message);
			}
			handleMessageInternal(message);
		}
		else {
			logger.debug(() -> "No message in the Message Store to release: " + message +
					". Likely another instance has already released it.");
		}
	}

	private boolean removeDelayedMessageFromMessageStore(Message<?> message) {
		if (this.messageStore instanceof SimpleMessageStore) {
			synchronized (this.messageGroupId) {
				Collection<Message<?>> messages = this.messageStore.getMessageGroup(this.messageGroupId).getMessages();
				if (messages.contains(message)) {
					this.messageStore.removeMessagesFromGroup(this.messageGroupId, message);
					return true;
				}
				else {
					return false;
				}
			}
		}
		else {
			return ((MessageStore) this.messageStore).removeMessage(message.getHeaders().getId()) != null;
		}
	}

	@Override
	public int getDelayedMessageCount() {
		return this.messageStore.messageGroupSize(this.messageGroupId);
	}

	/**
	 * Used for reading persisted Messages in the 'messageStore' to reschedule them e.g.
	 * upon application restart. The logic is based on iteration over
	 * {@code messageGroup.getMessages()} and schedules task for 'delay' logic. This
	 * behavior is dictated by the avoidance of invocation thread overload.
	 */
	@Override
	public synchronized void reschedulePersistedMessages() {
		MessageGroup messageGroup = this.messageStore.getMessageGroup(this.messageGroupId);
		try (Stream<Message<?>> messageStream = messageGroup.streamMessages()) {
			TaskScheduler taskScheduler = getTaskScheduler();
			messageStream.forEach((message) -> // NOSONAR
					taskScheduler.schedule(() -> {
						// This is fine to keep the reference to the message,
						// because the scheduled task is performed immediately.
						long delay = determineDelayForMessage(message);
						if (delay > 0) {
							releaseMessageAfterDelay(message, delay);
						}
						else {
							releaseMessage(message);
						}
					}, new Date()));
		}
	}

	/**
	 * Handle {@link ContextRefreshedEvent} to invoke
	 * {@link #reschedulePersistedMessages} as late as possible after application context
	 * startup. Also it checks {@link #initialized} to ignore other
	 * {@link ContextRefreshedEvent}s which may be published in the 'parent-child'
	 * contexts, e.g. in the Spring-MVC applications.
	 * @param event - {@link ContextRefreshedEvent} which occurs after Application context
	 * is completely initialized.
	 * @see #reschedulePersistedMessages
	 */
	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		if (event.getApplicationContext().equals(getApplicationContext())
				&& !this.initialized.getAndSet(true)) {
			reschedulePersistedMessages();
		}
	}

	/**
	 * Delegate {@link MessageHandler} implementation for 'release Message task'. Used as
	 * 'pointcut' to wrap 'release Message task' with <code>adviceChain</code>.
	 *
	 * @see #createReleaseMessageTask
	 * @see #releaseMessage
	 */
	private class ReleaseMessageHandler implements MessageHandler {

		ReleaseMessageHandler() {
		}

		@Override
		public void handleMessage(Message<?> message) throws MessagingException {
			DelayHandler.this.doReleaseMessage(message);
		}

	}

	public static final class DelayedMessageWrapper implements Serializable {

		private static final long serialVersionUID = -4739802369074947045L;

		private final long requestDate;

		private final Message<?> original;

		DelayedMessageWrapper(Message<?> original, long requestDate) {
			this.original = original;
			this.requestDate = requestDate;
		}

		public long getRequestDate() {
			return this.requestDate;
		}

		public Message<?> getOriginal() {
			return this.original;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o) {
				return true;
			}
			if (o == null || getClass() != o.getClass()) {
				return false;
			}

			DelayedMessageWrapper that = (DelayedMessageWrapper) o;

			return this.original.equals(that.original);
		}

		@Override
		public int hashCode() {
			return this.original.hashCode();
		}

	}

}
