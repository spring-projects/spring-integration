/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.core;

import org.springframework.integration.IntegrationPattern;
import org.springframework.integration.IntegrationPatternType;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;

/**
 * Base interface for any source of {@link Message Messages} that can be polled.
 *
 * @param <T> the expected payload type.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 */
@FunctionalInterface
public interface MessageSource<T> extends IntegrationPattern {

	/**
	 * Retrieve the next available message from this source.
	 * Returns {@code null} if no message is available.
	 * @return The message or null.
	 */
	@Nullable
	Message<T> receive();

	@Override
	default IntegrationPatternType getIntegrationPatternType() {
		return IntegrationPatternType.inbound_channel_adapter;
	}

}
