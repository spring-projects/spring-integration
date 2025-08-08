/*
 * Copyright © 2017 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2017-present the original author or authors.
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
