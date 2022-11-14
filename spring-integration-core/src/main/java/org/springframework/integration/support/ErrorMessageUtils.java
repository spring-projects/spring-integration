/*
 * Copyright 2017-2022 the original author or authors.
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
import org.springframework.core.AttributeAccessorSupport;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;

/**
 * Utilities for building error messages.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.3.10
 *
 */
public final class ErrorMessageUtils {

	/**
	 * The context key for the message object.
	 */
	public static final String FAILED_MESSAGE_CONTEXT_KEY = "message";

	/**
	 * The context key for the message object.
	 */
	public static final String INPUT_MESSAGE_CONTEXT_KEY = "inputMessage";

	private ErrorMessageUtils() {
	}

	/**
	 * Return a {@link AttributeAccessor} for the provided arguments.
	 * @param inputMessage the input message.
	 * @param failedMessage the failed message.
	 * @return the context.
	 */
	public static AttributeAccessor getAttributeAccessor(@Nullable Message<?> inputMessage,
			@Nullable Message<?> failedMessage) {

		AttributeAccessorSupport attributes = new ErrorMessageAttributes();
		if (inputMessage != null) {
			attributes.setAttribute(INPUT_MESSAGE_CONTEXT_KEY, inputMessage);
		}
		if (failedMessage != null) {
			attributes.setAttribute(FAILED_MESSAGE_CONTEXT_KEY, failedMessage);
		}
		return attributes;
	}

	@SuppressWarnings("serial")
	private static class ErrorMessageAttributes extends AttributeAccessorSupport {

		ErrorMessageAttributes() {
		}

	}

}
