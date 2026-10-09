/*
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.stomp.event;

import org.jspecify.annotations.Nullable;

/**
 * The {@link StompIntegrationEvent} implementation for the exception from STOMP Adapters.
 *
 * @author Artem Bilan
 * @since 4.2
 */
@SuppressWarnings("serial")
public class StompExceptionEvent extends StompIntegrationEvent {

	public StompExceptionEvent(Object source, @Nullable Throwable cause) {
		super(source, cause);
	}

}
