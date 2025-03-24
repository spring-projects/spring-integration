/*
 * Copyright 2025 the original author or authors.
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

package org.springframework.integration.handler.advice;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.locks.Lock;
import java.util.function.Function;

import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.expression.FunctionExpression;
import org.springframework.integration.expression.ValueExpression;
import org.springframework.integration.support.locks.LockRegistry;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.util.Assert;

/**
 * The {@link AbstractRequestHandlerAdvice} to ensure exclusive access to the
 * {@code AbstractReplyProducingMessageHandler.RequestHandler#handleRequestMessage(Message)} calls
 * based on the {@code lockKey} from message.
 * <p>
 * If {@code lockKey} for the message is {@code null}, the no locking around the call.
 * However, if {@link #setDiscardChannel(MessageChannel)} is provided, such a message will be sent there instead.
 *
 * @author Artem Bilan
 *
 * @since 6.5
 */
public class LockRequestHandlerAdvice extends AbstractRequestHandlerAdvice {

	private final LockRegistry lockRegistry;

	private final Expression lockKeyExpression;

	@Nullable
	private MessageChannel discardChannel;

	@Nullable
	private Expression waitLockDurationExpression;

	private EvaluationContext evaluationContext;

	/**
	 * Construct an advice instance based on a {@link LockRegistry} and fixed (shared) lock key.
	 * @param lockRegistry the {@link LockRegistry} to use.
	 * @param lockKey the static (shared) lock key for all the calls.
	 */
	public LockRequestHandlerAdvice(LockRegistry lockRegistry, Object lockKey) {
		this(lockRegistry, new ValueExpression<>(lockKey));
	}

	/**
	 * Construct an advice instance based on a {@link LockRegistry}
	 * and SpEL expression for the lock key against request message.
	 * @param lockRegistry the {@link LockRegistry} to use.
	 * @param lockKeyExpression the SpEL expression to evaluate a lock key against request message.
	 */
	public LockRequestHandlerAdvice(LockRegistry lockRegistry, Expression lockKeyExpression) {
		Assert.notNull(lockRegistry, "'lockRegistry' must not be null");
		Assert.notNull(lockKeyExpression, "'lockKeyExpression' must not be null");
		this.lockRegistry = lockRegistry;
		this.lockKeyExpression = lockKeyExpression;
	}

	/**
	 * Construct an advice instance based on a {@link LockRegistry}
	 * and function for the lock key against request message.
	 * @param lockRegistry the {@link LockRegistry} to use.
	 * @param lockKeyFunction the function to evaluate a lock key against request message.
	 */
	public LockRequestHandlerAdvice(LockRegistry lockRegistry, Function<Message<?>, Object> lockKeyFunction) {
		Assert.notNull(lockRegistry, "'lockRegistry' must not be null");
		Assert.notNull(lockKeyFunction, "'lockKeyFunction' must not be null");
		this.lockRegistry = lockRegistry;
		this.lockKeyExpression = new FunctionExpression<>(lockKeyFunction);
	}

	/**
	 * Optional duration for a {@link Lock#tryLock(long, TimeUnit)} API.
	 * Otherwise, {@link Lock#lockInterruptibly()} is used.
	 * @param waitLockDuration the duration for {@link Lock#tryLock(long, TimeUnit)}.
	 */
	public void setWaitLockDuration(Duration waitLockDuration) {
		setWaitLockDurationExpression(new ValueExpression<>(waitLockDuration));
	}

	/**
	 * The SpEL expression to evaluate a {@link Lock#tryLock(long, TimeUnit)} duration
	 * against request message.
	 * Can be evaluated to {@link Duration}, {@code long} (with meaning as milliseconds),
	 * or to string in the duration ISO-8601 format.
	 * @param waitLockDurationExpression SpEL expression for duration.
	 */
	public void setWaitLockDurationExpression(Expression waitLockDurationExpression) {
		this.waitLockDurationExpression = waitLockDurationExpression;
	}

	/**
	 * The SpEL expression to evaluate a {@link Lock#tryLock(long, TimeUnit)} duration
	 * against request message.
	 * Can be evaluated to {@link Duration}, {@code long} (with meaning as milliseconds),
	 * or to string in the duration ISO-8601 format.
	 * @param waitLockDurationExpression SpEL expression for duration.
	 */
	public void setWaitLockDurationExpressionString(String waitLockDurationExpression) {
		this.waitLockDurationExpression = EXPRESSION_PARSER.parseExpression(waitLockDurationExpression);
	}

	/**
	 * The function to evaluate a {@link Lock#tryLock(long, TimeUnit)} duration
	 * against request message.
	 * @param waitLockDurationFunction the function for duration.
	 */
	public void setWaitLockDurationFunction(Function<Message<?>, Duration> waitLockDurationFunction) {
		this.waitLockDurationExpression = new FunctionExpression<>(waitLockDurationFunction);
	}

	/**
	 * Set a channel where to send a message for which {@code lockKey} is evaluated to {@code null}.
	 * If this is not set and {@code lockKey == null}, no locking around the call.
	 * @param discardChannel the channel to send messages without a key.
	 */
	public void setDiscardChannel(@Nullable MessageChannel discardChannel) {
		this.discardChannel = discardChannel;
	}

	@Override
	protected void onInit() {
		super.onInit();
		this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(getBeanFactory());
	}

	@Override
	protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) {
		Object lockKey = this.lockKeyExpression.getValue(this.evaluationContext, message);
		if (lockKey != null) {
			Duration waitLockDuration = getWaitLockDuration(message);
			try {
				if (waitLockDuration == null) {
					return this.lockRegistry.executeLocked(lockKey, callback::execute);
				}
				else {
					return this.lockRegistry.executeLocked(lockKey, waitLockDuration, callback::execute);
				}
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				throw IntegrationUtils.wrapInHandlingExceptionIfNecessary(message,
						() -> "The lock for message was interrupted", ex);
			}
			catch (TimeoutException ex) {
				throw IntegrationUtils.wrapInHandlingExceptionIfNecessary(message,
						() -> "Could not acquire the lock in time: " + waitLockDuration, ex);
			}
		}
		else {
			if (this.discardChannel != null) {
				this.discardChannel.send(message);
				return null;
			}
			else {
				return callback.execute();
			}
		}
	}

	@Nullable
	private Duration getWaitLockDuration(Message<?> message) {
		if (this.waitLockDurationExpression != null) {
			Object value = this.waitLockDurationExpression.getValue(this.evaluationContext, message);
			if (value != null) {
				if (value instanceof Duration duration) {
					return duration;
				}
				else if (value instanceof Long aLong) {
					return Duration.ofMillis(aLong);
				}
				else {
					return Duration.parse(value.toString());
				}
			}
		}
		return null;
	}

}
