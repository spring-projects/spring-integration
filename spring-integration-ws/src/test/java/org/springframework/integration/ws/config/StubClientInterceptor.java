/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.ws.config;

import org.springframework.ws.client.WebServiceClientException;
import org.springframework.ws.client.support.interceptor.ClientInterceptor;
import org.springframework.ws.context.MessageContext;

/**
 * @author Mark Fisher
 */
public class StubClientInterceptor implements ClientInterceptor {

	public boolean handleRequest(MessageContext context) throws WebServiceClientException {
		return true;
	}

	public boolean handleResponse(MessageContext context) throws WebServiceClientException {
		return true;
	}

	public boolean handleFault(MessageContext context) throws WebServiceClientException {
		return false;
	}

	public void afterCompletion(MessageContext messageContext, Exception ex) throws WebServiceClientException {

	}

}
