/*
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.stomp.event;

import org.jspecify.annotations.Nullable;

/**
 * The {@link StompIntegrationEvent} implementation for the failed connection exceptions.
 *
 * @author Artem Bilan
 * @since 4.2.2
 */
@SuppressWarnings("serial")
public class StompConnectionFailedEvent extends StompIntegrationEvent {

	public StompConnectionFailedEvent(Object source, @Nullable Throwable cause) {
		super(source, cause);
	}

}
