/*
 * Copyright 2017-2019 the original author or authors.
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

package org.springframework.integration.support;

import org.springframework.core.AttributeAccessor;
import org.springframework.lang.Nullable;
import org.springframework.messaging.support.ErrorMessage;

/**
 * A strategy to build an {@link ErrorMessage} based on the provided
 * {@link Throwable} and {@link AttributeAccessor} as a context.
 * <p>
 * The {@code Throwable payload} is typically {@link org.springframework.messaging.MessagingException}
 * which {@code failedMessage} property can be used to determine a cause of the error.
 * <p>
 * This strategy can be used for the
 * {@link org.springframework.integration.handler.advice.ErrorMessageSendingRecoverer}
 * for {@link org.springframework.retry.RetryContext} access.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 4.3.10
 */
@FunctionalInterface
public interface ErrorMessageStrategy {

	/**
	 * Build the error message.
	 * @param payload the payload.
	 * @param attributes the attributes.
	 * @return the ErrorMessage.
	 */
	ErrorMessage buildErrorMessage(Throwable payload, @Nullable AttributeAccessor attributes);

}
