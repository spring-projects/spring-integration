/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.ws.config;

import java.io.IOException;

import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.client.core.FaultMessageResolver;

/**
 * @author Mark Fisher
 */
public class StubFaultMessageResolver implements FaultMessageResolver {

	public void resolveFault(WebServiceMessage message) throws IOException {
	}

}
