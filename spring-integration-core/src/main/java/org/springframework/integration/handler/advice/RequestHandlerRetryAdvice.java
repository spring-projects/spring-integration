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
package org.springframework.integration.handler.advice;

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
 * @since 2.2
 */
public class RequestHandlerRetryAdvice extends AbstractRequestHandlerAdvice
		implements RetryListener {

	private volatile RetryTemplate retryTemplate = new RetryTemplate();

	private volatile RecoveryCallback<Object> recoveryCallback;

	private static final ThreadLocal<Message<?>> messageHolder = new ThreadLocal<Message<?>>();

	// Stateless unless a state generator is provided
	private volatile RetryStateGenerator retryStateGenerator =
			new RetryStateGenerator() {
				@Override
				public RetryState determineRetryState(Message<?> message) {
					return null;
				}
			};

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
	protected void onInit() throws Exception {
		super.onInit();
		this.retryTemplate.registerListener(this);
	}

	@Override
	protected Object doInvoke(final ExecutionCallback callback, Object target, final Message<?> message) throws Exception {
		RetryState retryState = null;
		retryState = this.retryStateGenerator.determineRetryState(message);
		messageHolder.set(message);

		try {
			return retryTemplate.execute(new RetryCallback<Object, Exception>() {
				@Override
				public Object doWithRetry(RetryContext context) throws Exception {
					return callback.cloneAndExecute();
				}
			}, this.recoveryCallback, retryState);
		}
		catch (MessagingException e) {
			if (e.getFailedMessage() == null) {
				throw new MessagingException(message, e);
			}
			throw e;
		}
		catch (Exception e) {
			throw new MessagingException(message, "Failed to invoke handler",
					unwrapExceptionIfNecessary(e));
		}
		finally {
			messageHolder.remove();
		}
	}

	@Override
	public <T, E extends Throwable> boolean open(RetryContext context, RetryCallback<T, E> callback) {
		context.setAttribute("message", messageHolder.get());
		return true;
	}

	@Override
	public <T, E extends Throwable> void close(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
	}

	@Override
	public <T, E extends Throwable> void onError(RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
	}

}
