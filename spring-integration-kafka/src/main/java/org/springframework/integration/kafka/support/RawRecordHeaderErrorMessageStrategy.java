/*
 * Copyright © 2017 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2017-present the original author or authors.
 */

package org.springframework.integration.kafka.support;

import java.util.HashMap;
import java.util.Map;

import org.springframework.core.AttributeAccessor;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.support.ErrorMessageStrategy;
import org.springframework.integration.support.ErrorMessageUtils;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.ErrorMessage;

/**
 * {@link ErrorMessageStrategy} extension that adds the raw record as
 * a header to the {@link ErrorMessage}.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.4
 *
 */
public class RawRecordHeaderErrorMessageStrategy implements ErrorMessageStrategy {

	@Override
	public ErrorMessage buildErrorMessage(Throwable throwable, @Nullable AttributeAccessor context) {
		Object inputMessage = null;
		Map<String, Object> headers = new HashMap<>();
		if (context != null) {
			inputMessage = context.getAttribute(ErrorMessageUtils.INPUT_MESSAGE_CONTEXT_KEY);
			headers.put(KafkaHeaders.RAW_DATA, context.getAttribute(KafkaHeaders.RAW_DATA));
			headers.put(IntegrationMessageHeaderAccessor.SOURCE_DATA, context.getAttribute(KafkaHeaders.RAW_DATA));
		}
		if (inputMessage instanceof Message) {
			return new ErrorMessage(throwable, headers, (Message<?>) inputMessage);
		}
		else {
			return new ErrorMessage(throwable, headers);
		}
	}

}
