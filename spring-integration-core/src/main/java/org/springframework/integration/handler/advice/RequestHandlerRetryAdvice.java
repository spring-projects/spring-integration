/*
 * Copyright 2002-2023 the original author or authors.
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

import org.springframework.integration.support.ErrorMessageUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessagingException;
import org.springframework.retry.RecoveryCallback;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.RetryState;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.util.Assert;

/**
 * Uses spring-retry to perform stateless or stateful retry.
 * Stateless retry means the retries are performed internally
 * by the {@link RetryTemplate}; stateful retry means the
 * exception is thrown but state is maintained to support
 * the retry policies. Stateful retry requires a
 * {@link RetryStateGenerator}.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.2
 */
public class RequestHandlerRetryAdvice extends AbstractRequestHandlerAdvice
		implements RetryListener {

	private static final IntegrationRetryListener INTEGRATION_RETRY_LISTENER = new IntegrationRetryListener();

	private RetryTemplate retryTemplate = new RetryTemplate();

	private RecoveryCallback<Object> recoveryCallback;

	// Stateless unless a state generator is provided
	private RetryStateGenerator retryStateGenerator = message -> null;

	/**
	 * Set the retry template. Cause traversal should be enabled in the retry policy
	 * because user exceptions may be wrapped in a {@link MessagingException}.
	 * @param retryTemplate the retry template.
	 */
	public void setRetryTemplate(RetryTemplate retryTemplate) {
		Assert.notNull(retryTemplate, "'retryTemplate' cannot be null");
		this.retryTemplate = retryTemplate;
	}

	public void setRecoveryCallback(RecoveryCallback<Object> recoveryCallback) {
		this.recoveryCallback = recoveryCallback;
	}

	public void setRetryStateGenerator(RetryStateGenerator retryStateGenerator) {
		Assert.notNull(retryStateGenerator, "'retryStateGenerator' cannot be null");
		this.retryStateGenerator = retryStateGenerator;
	}

	@Override
	protected void onInit() {
		super.onInit();
		this.retryTemplate.registerListener(INTEGRATION_RETRY_LISTENER);
	}

	@Override
	protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) {
		IntegrationRetryCallback retryCallback = new IntegrationRetryCallback(message, callback);
		RetryState retryState = this.retryStateGenerator.determineRetryState(message);
		try {
			return this.retryTemplate.execute(retryCallback, this.recoveryCallback, retryState);
		}
		catch (MessagingException ex) {
			if (ex.getFailedMessage() == null) {
				throw new MessagingException(message, "Failed to invoke handler", ex);
			}
			throw ex;
		}
		catch (ThrowableHolderException ex) { // NOSONAR catch and rethrow
			throw ex;
		}
		catch (Exception ex) {
			throw new ThrowableHolderException(ex);
		}
	}

	/**
	 * Set a {@link ErrorMessageUtils#FAILED_MESSAGE_CONTEXT_KEY} attribute into context.
	 * @param context the current {@link RetryContext}.
	 * @param callback the current {@link RetryCallback}.
	 * @param <T> the type of object returned by the callback
	 * @param <E> the type of exception it declares may be thrown
	 * @return the open state.
	 * @deprecated since 6.2 in favor of an internal {@link RetryListener} implementation.
	 * The {@link RequestHandlerRetryAdvice} must not be used as a listener for external {@link RetryTemplate}
	 * instances.
	 */
	@Deprecated(since = "6.2", forRemoval = true)
	@Override
	public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
		return INTEGRATION_RETRY_LISTENER.open(context, callback);
	}

	private static class IntegrationRetryListener implements RetryListener {

		IntegrationRetryListener() {
		}

		@Override
		public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
			Assert.state(callback instanceof IntegrationRetryCallback,
					"A 'RequestHandlerRetryAdvice' cannot be used as a 'RetryListener'");
			context.setAttribute(ErrorMessageUtils.FAILED_MESSAGE_CONTEXT_KEY,
					((IntegrationRetryCallback) callback).messageToTry);
			return true;
		}

	}

	private record IntegrationRetryCallback(Message<?> messageToTry, ExecutionCallback callback)
			implements RetryCallback<Object, Exception> {

		@Override
		public Object doWithRetry(RetryContext context) {
			return this.callback.cloneAndExecute();
		}

	}

}
