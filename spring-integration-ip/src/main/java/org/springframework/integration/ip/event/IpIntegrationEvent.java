/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.ip.event;

import org.springframework.integration.events.IntegrationEvent;

/**
 * @author Gary Russell
 *
 * @since 3.0
 *
 */
@SuppressWarnings("serial")
public abstract class IpIntegrationEvent extends IntegrationEvent {

	public IpIntegrationEvent(Object source) {
		super(source);
	}

	public IpIntegrationEvent(Object source, Throwable cause) {
		super(source, cause);
	}

}
