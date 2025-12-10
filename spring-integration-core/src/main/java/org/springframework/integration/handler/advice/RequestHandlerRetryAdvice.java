/*
 * Copyright 2002-present the original author or authors.
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

import java.io.Serial;
import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import org.jspecify.annotations.Nullable;

import org.springframework.core.AttributeAccessor;
import org.springframework.core.retry.RetryException;
import org.springframework.core.retry.RetryListener;
import org.springframework.core.retry.RetryPolicy;
import org.springframework.core.retry.RetryTemplate;
import org.springframework.core.retry.Retryable;
import org.springframework.integration.core.RecoveryCallback;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.support.ErrorMessageUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.util.backoff.BackOff;
import org.springframework.util.backoff.BackOffExecution;

/**
 * The {@link AbstractRequestHandlerAdvice} implementation for retrying the target
 * {@link org.springframework.messaging.MessageHandler} execution.
 * <p>
 * By default, this advice performs 3 attempts (plus initial execution) with no delay in between.
 * <p>
 * If {@link #setStateKeyFunction(Function)} is provided, the retry logic is turned into a stateful algorithm.
 * In this case, a {@link BackOffExecution} is cached by the mentioned retry state key if an exception is retryable.
 * Then this exception is thrown back to the caller, e.g.,
 * for transaction rollback or re-queueing the message onto message broker.
 * The next time when a message with the same key arrives again (redelivered),
 * the cached {@link BackOffExecution} is restored, and the target service is called again
 * immediately according to the {@link RetryPolicy}.
 * <p>
 * If the {@link #setNewMessagePredicate(Predicate)} returns {@code true},
 * a new stateful retry cycle is started even if a state for this message is already cached.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.2
 */
public class RequestHandlerRetryAdvice extends AbstractRequestHandlerAdvice {

	private final RetryTemplate retryTemplate = new RetryTemplate();

	private final Map<Object, RetryState> retryStateCache =
			Collections.synchronizedMap(new LinkedHashMap<>(100, 0.75f, true) {

				@Override
				protected boolean removeEldestEntry(Map.Entry<Object, RetryState> eldest) {
					return size() > RequestHandlerRetryAdvice.this.stateCacheSize;
				}

			});

	private RetryPolicy retryPolicy = RetryPolicy.builder().delay(Duration.ZERO).build();

	private RetryListener retryListener = new RetryListener() {

	};

	private @Nullable RecoveryCallback<Object> recoveryCallback;

	@Nullable
	private Function<Message<?>, Object> stateKeyFunction;

	private int stateCacheSize = 100;

	private Predicate<Message<?>> newMessagePredicate = message -> false;

	public RequestHandlerRetryAdvice() {
		this.retryTemplate.setRetryPolicy(this.retryPolicy);
	}

	/**
	 * Set a {@link RetryPolicy} to use.
	 * Defaults to 3 attempts with no delay in between.
	 * Mutually exclusive with {@link #setBackOff(BackOff)}.
	 * @param retryPolicy the policy.
	 */
	public void setRetryPolicy(RetryPolicy retryPolicy) {
		this.retryPolicy = retryPolicy;
		this.retryTemplate.setRetryPolicy(this.retryPolicy);
	}

	/**
	 * Set a {@link BackOff} to be used in the internal {@link RetryPolicy} with rest options as defaults.
	 * Mutually exclusive with {@link #setRetryPolicy(RetryPolicy)}.
	 * @param backOff the back-off.
	 * @since 7.0.1
	 */
	public void setBackOff(BackOff backOff) {
		this.retryPolicy = RetryPolicy.builder().backOff(backOff).build();
		this.retryTemplate.setRetryPolicy(this.retryPolicy);
	}

	/**
	 * Set a {@link RetryListener} to track retry cycle phases.
	 * @param retryListener the listener to use.
	 */
	public void setRetryListener(RetryListener retryListener) {
		this.retryListener = retryListener;
		this.retryTemplate.setRetryListener(this.retryListener);
	}

	/**
	 * Set a {@link RecoveryCallback} to handle a {@link RetryException}.
	 * @param recoveryCallback the callback to use.
	 */
	public void setRecoveryCallback(RecoveryCallback<Object> recoveryCallback) {
		this.recoveryCallback = recoveryCallback;
	}

	/**
	 * Set a {@link Function} to determine a stateful retry execution key against a request message.
	 * If not provided, the retry behavior is stateless.
	 * @param stateKeyFunction the function to determine a stateful retry execution key against a request message.
	 */
	public void setStateKeyFunction(Function<Message<?>, Object> stateKeyFunction) {
		this.stateKeyFunction = stateKeyFunction;
	}

	/**
	 * The size of the stateful retry state cache.
	 * Defaults to 100.
	 * This option is used only when {@link #setStateKeyFunction(Function)} is provided.
	 * @param stateCacheSize the size of the cache.
	 */
	public void setStateCacheSize(int stateCacheSize) {
		this.stateCacheSize = stateCacheSize;
	}

	/**
	 * Set a {@link Predicate} to determine if a fresh stateful retry state should be started for this request message.
	 * {@code false} by default to not refresh existing stateful retry states.
	 * This option is used only when {@link #setStateKeyFunction(Function)} is provided.
	 * @param newMessagePredicate the predicate to use.
	 */
	public void setNewMessagePredicate(Predicate<Message<?>> newMessagePredicate) {
		this.newMessagePredicate = newMessagePredicate;
	}

	@Override
	protected @Nullable Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) {
		IntegrationRetryCallback retryCallback = new IntegrationRetryCallback(message, callback, target);
		AttributeAccessor retryContext = ErrorMessageUtils.getAttributeAccessor(message, message);
		try {
			if (this.stateKeyFunction == null) {
				return this.retryTemplate.<@Nullable Object>execute(retryCallback);
			}

			return statefulRetry(retryCallback, this.stateKeyFunction);
		}
		catch (RetryException ex) {
			if (this.recoveryCallback != null) {
				return this.recoveryCallback.recover(retryContext, ex);
			}

			Throwable cause = ex.getCause();

			if (cause instanceof MessagingException messagingException) {
				if (messagingException.getFailedMessage() == null) {
					throw new MessagingException(message, "Failed to invoke handler", ex);
				}
				throw messagingException;
			}
			else if (cause instanceof ThrowableHolderException throwableHolderException) {
				throw throwableHolderException;
			}

			throw new ThrowableHolderException(cause);
		}
	}

	private @Nullable Object statefulRetry(IntegrationRetryCallback retryCallback,
			Function<Message<?>, Object> retryStateKeyFunction) throws RetryException {

		Object retryStateKey = retryStateKeyFunction.apply(retryCallback.messageToTry);
		RetryState retryState = this.retryStateCache.get(retryStateKey);
		if (retryState == null || this.newMessagePredicate.test(retryCallback.messageToTry)) {
			try {
				return retryCallback.execute();
			}
			catch (Exception ex) {
				if (this.retryPolicy.shouldRetry(ex)) {
					this.retryStateCache.put(retryStateKey, new RetryState(this.retryPolicy.getBackOff().start(), ex));
				}
				else {
					this.retryStateCache.remove(retryStateKey);
				}
				throw ex;
			}
		}

		if (BackOffExecution.STOP != retryState.currentDelay) {
			try {
				Thread.sleep(retryState.currentDelay);
			}
			catch (InterruptedException interruptedException) {
				Thread.currentThread().interrupt();
				RetryException retryException = new RetryInterruptedException(
						"Unable to back off for retryable operation '%s'".formatted(retryCallback.getName()),
						interruptedException);
				retryState.exceptions.forEach(retryException::addSuppressed);
				this.retryListener.onRetryPolicyInterruption(this.retryPolicy, retryCallback, retryException);
				throw retryException;
			}

			try {
				this.retryListener.beforeRetry(this.retryPolicy, retryCallback);
				Object result = retryCallback.execute();
				this.retryStateCache.remove(retryStateKey);
				this.retryListener.onRetrySuccess(this.retryPolicy, retryCallback, result);
				return result;
			}
			catch (Throwable ex) {
				this.retryListener.onRetryFailure(this.retryPolicy, retryCallback, ex);
				if (this.retryPolicy.shouldRetry(ex)) {
					retryState.exceptions.add(ex);
					// Short-circuit at the end of the current attempt to avoid extra external future calls
					if (BackOffExecution.STOP != retryState.nextBackOff()) {
						throw ex;
					}
				}
				else {
					this.retryStateCache.remove(retryStateKey);
					throw ex;
				}
			}
		}

		// The RetryPolicy has exhausted at this point, so we throw a RetryException with the
		// last exception as the cause and remaining exceptions as suppressed exceptions.
		this.retryStateCache.remove(retryStateKey);
		RetryException retryException = new RetryException(
				"Retry policy for operation '%s' exhausted; aborting execution".formatted(retryCallback.getName()),
				retryState.exceptions.removeLast());
		retryState.exceptions.forEach(retryException::addSuppressed);
		this.retryListener.onRetryPolicyExhaustion(this.retryPolicy, retryCallback, retryException);
		throw retryException;
	}

	private record IntegrationRetryCallback(Message<?> messageToTry, ExecutionCallback callback, Object target)
			implements Retryable<@Nullable Object> {

		@Override
		public @Nullable Object execute() {
			return this.callback.cloneAndExecute();
		}

		@Override
		public String getName() {
			return this.target instanceof AbstractReplyProducingMessageHandler.RequestHandler requestHandler
					? requestHandler.getAdvisedHandler().getComponentName()
					: this.target.getClass().getName();
		}

	}

	private static class RetryState {

		private final Deque<Throwable> exceptions = new ArrayDeque<>(4);

		private final BackOffExecution backOffExecution;

		private volatile long currentDelay;

		RetryState(BackOffExecution backOffExecution, Throwable initialException) {
			this.backOffExecution = backOffExecution;
			this.currentDelay = this.backOffExecution.nextBackOff();
			this.exceptions.add(initialException);
		}

		long nextBackOff() {
			return this.currentDelay = this.backOffExecution.nextBackOff();
		}

	}

	private static class RetryInterruptedException extends RetryException {

		@Serial
		private static final long serialVersionUID = 1L;

		RetryInterruptedException(String message, InterruptedException cause) {
			super(message, cause);
		}

		@Override
		public int getRetryCount() {
			return (getSuppressed().length - 1);
		}

	}

}
