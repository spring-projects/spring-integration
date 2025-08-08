/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.support;

import org.springframework.messaging.Message;

/**
 * @author Gary Russell
 * @since 4.0
 *
 */
public class MutableMessageBuilderFactory implements MessageBuilderFactory {

	@Override
	public <T> MutableMessageBuilder<T> fromMessage(Message<T> message) {
		return MutableMessageBuilder.fromMessage(message);
	}

	@Override
	public <T> MutableMessageBuilder<T> withPayload(T payload) {
		return MutableMessageBuilder.withPayload(payload);
	}

}
