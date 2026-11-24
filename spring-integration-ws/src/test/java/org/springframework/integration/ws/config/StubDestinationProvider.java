/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.ws.config;

import java.net.URI;

import org.springframework.ws.client.support.destination.DestinationProvider;

/**
 * @author Jonas Partner
 */
public class StubDestinationProvider implements DestinationProvider {

	public URI getDestination() {
		return null;
	}

}
