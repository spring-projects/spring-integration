/*
 * Copyright © 2018 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2018-present the original author or authors.
 */

package org.springframework.integration.support;

import org.springframework.messaging.converter.MessageConverter;
import org.springframework.messaging.handler.annotation.support.PayloadMethodArgumentResolver;
import org.springframework.validation.Validator;

/**
 * A {@link PayloadMethodArgumentResolver} that treats KafkaNull payloads as null.
 * {@link org.springframework.messaging.handler.annotation.Payload @Paylaod}
 * annotation must have required = false.
 *
 * @author Gary Russell
 * @since 5.1
 *
 */
public class NullAwarePayloadArgumentResolver extends PayloadMethodArgumentResolver {

	public NullAwarePayloadArgumentResolver(MessageConverter messageConverter) {
		super(messageConverter, null, false);
	}

	public NullAwarePayloadArgumentResolver(MessageConverter messageConverter, Validator validator) {
		super(messageConverter, validator, false);
	}

	@Override
	protected boolean isEmptyPayload(Object payload) {
		return super.isEmptyPayload(payload) || "KafkaNull".equals(payload.getClass().getSimpleName());
	}

}
