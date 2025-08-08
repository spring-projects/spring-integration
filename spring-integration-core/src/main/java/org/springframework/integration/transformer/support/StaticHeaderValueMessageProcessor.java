/*
 * Copyright © 2013 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2013-present the original author or authors.
 */

package org.springframework.integration.transformer.support;

import org.springframework.messaging.Message;

/**
 * @param <T> the value type.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 *
 * @since 3.0
 */
public class StaticHeaderValueMessageProcessor<T> extends AbstractHeaderValueMessageProcessor<T> {

	private final T value;

	public StaticHeaderValueMessageProcessor(T value) {
		this.value = value;
	}

	@Override
	public T processMessage(Message<?> message) {
		return this.value;
	}

}
