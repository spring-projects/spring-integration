/*
 * Copyright © 2017 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2017-present the original author or authors.
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
