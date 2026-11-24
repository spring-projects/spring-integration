/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
