/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
