/*
 * Copyright 2013-present the original author or authors.
 */

package org.springframework.integration.transformer.support;

import org.jspecify.annotations.Nullable;

import org.springframework.messaging.Message;

/**
 * A {@link HeaderValueMessageProcessor} that returns a static value.
 * @param <T> the value type.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Glenn Renfro
 *
 * @since 3.0
 */
public class StaticHeaderValueMessageProcessor<T extends @Nullable Object> extends AbstractHeaderValueMessageProcessor<T> {

	private final T value;

	public StaticHeaderValueMessageProcessor(T value) {
		this.value = value;
	}

	@Override
	public T processMessage(Message<?> message) {
		return this.value;
	}

}
