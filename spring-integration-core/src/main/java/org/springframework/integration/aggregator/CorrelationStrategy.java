/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.aggregator;

import org.jspecify.annotations.Nullable;

import org.springframework.messaging.Message;

/**
 * Strategy for determining how messages can be correlated. Implementations
 * should return the correlation key value associated with a particular message.
 *
 * @author Marius Bogoevici
 * @author Iwein Fuld
 */
@FunctionalInterface
public interface CorrelationStrategy {

	/**
	 * Find the correlation key for the given message. If no key can be determined the strategy should not return
	 * <code>null</code>, but throw an exception.
	 *
	 * @param message The message.
	 * @return The correlation key.
	 */
	@Nullable
	Object getCorrelationKey(Message<?> message);

}
