/*
 * Copyright 2016-2020 the original author or authors.
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

package org.springframework.integration.dsl;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Function;

import org.aopalliance.aop.Advice;

import org.springframework.expression.Expression;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.handler.DelayHandler;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.transaction.TransactionInterceptorBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.transaction.TransactionManager;
import org.springframework.transaction.interceptor.TransactionInterceptor;
import org.springframework.util.Assert;

/**
 * A {@link ConsumerEndpointSpec} for a {@link DelayHandler}.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 5.0
 */
public class DelayerEndpointSpec extends ConsumerEndpointSpec<DelayerEndpointSpec, DelayHandler> {

	private final List<Advice> delayedAdvice = new LinkedList<>();

	protected DelayerEndpointSpec(DelayHandler delayHandler) {
		super(delayHandler);
		Assert.notNull(delayHandler, "'delayHandler' must not be null.");
		this.handler.setDelayedAdviceChain(this.delayedAdvice);
	}

	/**
	 * @param defaultDelay the defaultDelay.
	 * @return the endpoint spec.
	 * @see DelayHandler#setDefaultDelay(long)
	 */
	public DelayerEndpointSpec defaultDelay(long defaultDelay) {
		this.handler.setDefaultDelay(defaultDelay);
		return _this();
	}

	/**
	 * @param ignoreExpressionFailures the ignoreExpressionFailures.
	 * @return the endpoint spec.
	 * @see DelayHandler#setIgnoreExpressionFailures(boolean)
	 */
	public DelayerEndpointSpec ignoreExpressionFailures(boolean ignoreExpressionFailures) {
		this.handler.setIgnoreExpressionFailures(ignoreExpressionFailures);
		return _this();
	}

	/**
	 * @param messageStore the message store.
	 * @return the endpoint spec.
	 */
	public DelayerEndpointSpec messageStore(MessageGroupStore messageStore) {
		this.handler.setMessageStore(messageStore);
		return _this();
	}

	/**
	 * Configure a list of {@link Advice} objects that will be applied, in nested order,
	 * when delayed messages are sent.
	 * @param advice the advice chain.
	 * @return the endpoint spec.
	 */
	public DelayerEndpointSpec delayedAdvice(Advice... advice) {
		this.delayedAdvice.addAll(Arrays.asList(advice));
		return _this();
	}

	public DelayerEndpointSpec delayExpression(Expression delayExpression) {
		this.handler.setDelayExpression(delayExpression);
		return this;
	}

	public DelayerEndpointSpec delayExpression(String delayExpression) {
		this.handler.setDelayExpression(PARSER.parseExpression(delayExpression));
		return this;
	}

	/**
	 * Set a message channel to which an
	 * {@link org.springframework.messaging.support.ErrorMessage} will be sent if sending the
	 * released message fails. If the error flow returns normally, the release is complete.
	 * If the error flow throws an exception, the release will be re-attempted.
	 * If there is a transaction advice on the release task, the error flow is called
	 * within the transaction.
	 * @param channel the channel.
	 * @return the endpoint spec.
	 * @see #maxAttempts(int)
	 * @see #retryDelay(long)
	 * @since 5.0.8
	 */
	public DelayerEndpointSpec delayedMessageErrorChannel(MessageChannel channel) {
		this.handler.setDelayedMessageErrorChannel(channel);
		return this;
	}

	/**
	 * Set a message channel name to which an
	 * {@link org.springframework.messaging.support.ErrorMessage} will be sent if sending
	 * the released message fails. If the error flow returns normally, the release is
	 * complete. If the error flow throws an exception, the release will be re-attempted.
	 * If there is a transaction advice on the release task, the error flow is called
	 * within the transaction.
	 * @param channel the channel name.
	 * @return the endpoint spec.
	 * @see #maxAttempts(int)
	 * @see #retryDelay(long)
	 * @since 5.0.8
	 */
	public DelayerEndpointSpec delayedMessageErrorChannel(String channel) {
		this.handler.setDelayedMessageErrorChannelName(channel);
		return this;
	}

	/**
	 * Set the maximum number of release attempts for when message release fails.
	 * Default {@value DelayHandler#DEFAULT_MAX_ATTEMPTS}.
	 * @param maxAttempts the max attempts.
	 * @return the endpoint spec.
	 * @see #retryDelay(long)
	 * @since 5.0.8
	 */
	public DelayerEndpointSpec maxAttempts(int maxAttempts) {
		this.handler.setMaxAttempts(maxAttempts);
		return this;
	}

	/**
	 * Set an additional delay to apply when retrying after a release failure.
	 * Default {@value DelayHandler#DEFAULT_RETRY_DELAY}.
	 * @param retryDelay the retry delay.
	 * @return the endpoint spec.
	 * @see #maxAttempts(int)
	 * @since 5.0.8
	 */
	public DelayerEndpointSpec retryDelay(long retryDelay) {
		this.handler.setRetryDelay(retryDelay);
		return this;
	}

	/**
	 * Specify a {@link TransactionInterceptor} {@link Advice} with default
	 * {@link TransactionManager} and
	 * {@link org.springframework.transaction.interceptor.DefaultTransactionAttribute} for
	 * the
	 * {@link org.springframework.messaging.MessageHandler}.
	 * @return the spec.
	 * @since 5.0.8
	 */
	public DelayerEndpointSpec transactionalRelease() {
		TransactionInterceptor transactionInterceptor = new TransactionInterceptorBuilder().build();
		this.componentsToRegister.put(transactionInterceptor, null);
		return delayedAdvice(transactionInterceptor);
	}

	/**
	 * Specify a {@link TransactionInterceptor} {@link Advice} for the
	 * {@link org.springframework.messaging.MessageHandler}.
	 * @param transactionInterceptor the {@link TransactionInterceptor} to use.
	 * @return the spec.
	 * @see TransactionInterceptorBuilder
	 * @since 5.0.8
	 */
	public DelayerEndpointSpec transactionalRelease(TransactionInterceptor transactionInterceptor) {
		return delayedAdvice(transactionInterceptor);
	}

	/**
	 * Specify a {@link TransactionInterceptor} {@link Advice} with the provided
	 * {@link TransactionManager} and default
	 * {@link org.springframework.transaction.interceptor.DefaultTransactionAttribute}
	 * for the {@link org.springframework.messaging.MessageHandler}.
	 * @param transactionManager the {@link TransactionManager} to use.
	 * @return the spec.
	 * @since 5.2.5
	 */
	public DelayerEndpointSpec transactionalRelease(TransactionManager transactionManager) {
		return transactionalRelease(
				new TransactionInterceptorBuilder()
						.transactionManager(transactionManager)
						.build());
	}

	/**
	 * Specify the function to determine delay value against {@link Message}.
	 * Typically used with a Java 8 Lambda expression:
	 * <pre class="code">
	 * {@code
	 *  .<Foo>delay("delayer", m -> m.getPayload().getDate(),
	 *            c -> c.advice(this.delayedAdvice).messageStore(this.messageStore()))
	 * }
	 * </pre>
	 * @param delayFunction the {@link Function} to determine delay.
	 * @param <P> the payload type.
	 * @return the endpoint spec.
	 */
	public <P> DelayerEndpointSpec delayFunction(Function<Message<P>, Object> delayFunction) {
		this.handler.setDelayExpression(new FunctionExpression<>(delayFunction));
		return this;
	}

}
