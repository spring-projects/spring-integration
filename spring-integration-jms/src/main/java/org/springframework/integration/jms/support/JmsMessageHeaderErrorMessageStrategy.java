/*
 * Copyright © 2024 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2024-present the original author or authors.
 */

package org.springframework.integration.jms.support;

import java.util.HashMap;
import java.util.Map;

import org.springframework.core.AttributeAccessor;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.support.ErrorMessageStrategy;
import org.springframework.integration.support.ErrorMessageUtils;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.ErrorMessage;

/**
 * An {@link ErrorMessageStrategy} extension that adds the raw JMS message as
 * a header to the {@link ErrorMessage}.
 *
 * @author Artem Bilan
 *
 * @since 6.3
 *
 */
public class JmsMessageHeaderErrorMessageStrategy implements ErrorMessageStrategy {

	/**
	 * Header name/retry context variable for the raw received message.
	 */
	public static final String JMS_RAW_MESSAGE = "jms_raw_message";

	@Override
	public ErrorMessage buildErrorMessage(Throwable throwable, @Nullable AttributeAccessor context) {
		Object inputMessage = context == null ? null
				: context.getAttribute(ErrorMessageUtils.INPUT_MESSAGE_CONTEXT_KEY);
		Map<String, Object> headers = new HashMap<>();
		if (context != null) {
			headers.put(JMS_RAW_MESSAGE, context.getAttribute(JMS_RAW_MESSAGE));
			headers.put(IntegrationMessageHeaderAccessor.SOURCE_DATA, context.getAttribute(JMS_RAW_MESSAGE));
		}
		if (inputMessage instanceof Message) {
			return new ErrorMessage(throwable, headers, (Message<?>) inputMessage);
		}
		else {
			return new ErrorMessage(throwable, headers);
		}
	}

}
