/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.ws.config;

import java.io.IOException;
import java.net.URI;

import org.springframework.ws.transport.WebServiceConnection;
import org.springframework.ws.transport.WebServiceMessageSender;

/**
 *
 * @author Jonas Partner
 *
 */
public class StubMessageSender implements WebServiceMessageSender {

	public WebServiceConnection createConnection(URI uri) throws IOException {
		return null;
	}

	public boolean supports(URI uri) {
		return false;
	}

}
