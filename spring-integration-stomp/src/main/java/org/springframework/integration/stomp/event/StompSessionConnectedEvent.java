/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.stomp.event;

/**
 * The {@link StompIntegrationEvent} indicating the STOMP session establishment.
 *
 * @author Artem Bilan
 * @since 4.2.2
 */
@SuppressWarnings("serial")
public class StompSessionConnectedEvent extends StompIntegrationEvent {

	public StompSessionConnectedEvent(Object source) {
		super(source);
	}

}
