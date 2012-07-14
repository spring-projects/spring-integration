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

import org.springframework.integration.Message;
import org.springframework.integration.MessagingException;
import org.springframework.retry.RecoveryCallback;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryState;
import org.springframework.retry.support.RetryTemplate;

/**
 * Uses spring-retry to perform stateless or stateful retry.
 * Stateless retry means the retries are performed internally
 * by the {@link RetryTemplate}; stateful retry means the
 * exception is thrown but state is maintained to support
 * the retry policies. Stateful retry requires a
 * {@link RetryStateGenerator}.
 * @author Gary Russell
 * @since 2.2
 *
 */
public class RequestHandlerRetryAdvice extends AbstractRequestHandlerAdvice {

	private volatile RetryTemplate retryTemplate = new RetryTemplate();

	private volatile RecoveryCallback<Object> recoveryCallback;

	// Stateless unless a state generator is provided
	private volatile RetryStateGenerator retryStateGenerator =
			new RetryStateGenerator() {
				public RetryState determineRetryState(Message<?> message) {
					return null;
				}
			};

	public void setRetryTemplate(RetryTemplate retryTemplate) {
		this.retryTemplate = retryTemplate;
	}

	public void setRecoveryCallback(RecoveryCallback<Object> recoveryCallback) {
		this.recoveryCallback = recoveryCallback;
	}

	public void setRetryStateGenerator(RetryStateGenerator retryStateGenerator) {
		this.retryStateGenerator = retryStateGenerator;
	}

	@Override
	protected Object doInvoke(final ExecutionCallback callback, Object target, final Message<?> message) throws Throwable {
		RetryState retryState = null;
		retryState = this.retryStateGenerator.determineRetryState(message);

		return retryTemplate.execute(new RetryCallback<Object>(){
			public Object doWithRetry(RetryContext context) throws Exception {
				try {
					return callback.execute();
				}
				catch (MessagingException e) {
					if (e.getFailedMessage() == null) {
						e.setFailedMessage(message);
					}
					throw e;
				}
				catch (Throwable t) {
					throw new MessagingException(message, "Failed to invoke handler", t);
				}
			}
		}, this.recoveryCallback, retryState);
	}

}
