/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.aggregator;

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
	Object getCorrelationKey(Message<?> message);

}
