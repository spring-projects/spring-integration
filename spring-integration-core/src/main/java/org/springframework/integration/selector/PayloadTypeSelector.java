/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.selector;

import java.util.ArrayList;
import java.util.List;

import org.springframework.integration.core.MessageSelector;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * A {@link MessageSelector} implementation that checks the type of the
 * {@link Message} payload. The payload type must be assignable to at least one
 * of the selector's accepted types.
 *
 * @author Mark Fisher
 */
public class PayloadTypeSelector implements MessageSelector {

	private final List<Class<?>> acceptedTypes = new ArrayList<Class<?>>();

	/**
	 * Create a selector for the provided types. At least one is required.
	 *
	 * @param types The types.
	 */
	public PayloadTypeSelector(Class<?>... types) {
		Assert.notEmpty(types, "at least one type is required");
		for (Class<?> type : types) {
			this.acceptedTypes.add(type);
		}
	}

	@Override
	public boolean accept(Message<?> message) {
		Assert.notNull(message, "'message' must not be null");
		Object payload = message.getPayload();
		Assert.notNull(payload, "'payload' must not be null");
		for (Class<?> type : this.acceptedTypes) {
			if (type.isAssignableFrom(payload.getClass())) {
				return true;
			}
		}
		return false;
	}

}
