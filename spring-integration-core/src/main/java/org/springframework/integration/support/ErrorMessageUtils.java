/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.support;

import org.jspecify.annotations.Nullable;

import org.springframework.core.AttributeAccessor;
import org.springframework.core.AttributeAccessorSupport;
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
