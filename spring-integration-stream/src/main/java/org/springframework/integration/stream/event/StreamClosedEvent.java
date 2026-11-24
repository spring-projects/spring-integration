/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.stream.event;

import org.springframework.integration.events.IntegrationEvent;

/**
 * Application event published when EOF is detected on a stream.
 *
 * @author Gary Russell
 *
 * @since 7.0
 *
 */
@SuppressWarnings("serial")
public class StreamClosedEvent extends IntegrationEvent {

	public StreamClosedEvent(Object source) {
		super(source);
	}

}
