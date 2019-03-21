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
import org.springframework.messaging.Message;
import org.springframework.messaging.support.ErrorMessage;

/**
 * A simple {@link ErrorMessageStrategy} implementations which produces
 * a error message with original message if the {@link AttributeAccessor} has
 * {@link ErrorMessageUtils#INPUT_MESSAGE_CONTEXT_KEY} attribute.
 * Otherwise plain {@link ErrorMessage} with the {@code throwable} as {@code payload}.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.3.10
 *
 * @see ErrorMessageUtils
 */
public class DefaultErrorMessageStrategy implements ErrorMessageStrategy {

	@Override
	public ErrorMessage buildErrorMessage(Throwable throwable, @Nullable AttributeAccessor attributes) {
		Object inputMessage = attributes == null ? null
				: attributes.getAttribute(ErrorMessageUtils.INPUT_MESSAGE_CONTEXT_KEY);
		if (inputMessage instanceof Message) {
			return new ErrorMessage(throwable, (Message<?>) inputMessage);
		}
		else {
			return new ErrorMessage(throwable);
		}
	}

}
