/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.ws.config;

import java.io.IOException;

import javax.xml.transform.TransformerException;

import org.springframework.ws.WebServiceMessage;
import org.springframework.ws.client.core.WebServiceMessageCallback;

/**
 * @author Mark Fisher
 */
public class StubWebServiceMessageCallback implements WebServiceMessageCallback {

	public void doWithMessage(WebServiceMessage message) throws IOException, TransformerException {
	}

}
