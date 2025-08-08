/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
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
