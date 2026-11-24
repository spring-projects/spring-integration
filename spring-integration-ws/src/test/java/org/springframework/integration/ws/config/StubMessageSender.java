/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.ws.config;

import java.net.URI;

import org.springframework.ws.transport.WebServiceConnection;
import org.springframework.ws.transport.WebServiceMessageSender;

/**
 *
 * @author Jonas Partner
 * @author Artem Bilan
 */
public class StubMessageSender implements WebServiceMessageSender {

	public WebServiceConnection createConnection(URI uri) {
		return null;
	}

	@Override
	public boolean supports(URI uri, UriSource uriSource) {
		return false;
	}

}
