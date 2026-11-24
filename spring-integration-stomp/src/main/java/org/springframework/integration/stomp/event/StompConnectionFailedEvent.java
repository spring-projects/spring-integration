/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
