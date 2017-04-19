/*
 * Copyright 2017 the original author or authors.
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

package org.springframework.integration.support;

import org.springframework.core.AttributeAccessor;
import org.springframework.messaging.support.ErrorMessage;
import org.springframework.retry.RetryContext;

/**
 * A strategy to be used on the recovery function to produce
 * a {@link ErrorMessage} based on the {@link RetryContext}.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 4.3.10
 */
public interface ErrorMessageStrategy {

	/**
	 * Build the error message.
	 * @param payload the payload.
	 * @param attributes the attributes.
	 * @return the ErrorMessage.
	 */
	ErrorMessage buildErrorMessage(Throwable payload, AttributeAccessor attributes);

}
