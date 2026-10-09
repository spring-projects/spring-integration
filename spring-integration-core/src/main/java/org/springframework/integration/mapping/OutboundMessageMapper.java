/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.mapping;

import org.jspecify.annotations.Nullable;

import org.springframework.messaging.Message;

/**
 * Strategy interface for mapping from a {@link Message} to an Object.
 *
 * @param <T> the target result type.
 *
 * @author Mark Fisher
 * @author Gary Russell
 */
@FunctionalInterface
public interface OutboundMessageMapper<T> {

	@Nullable
	T fromMessage(Message<?> message);

}
