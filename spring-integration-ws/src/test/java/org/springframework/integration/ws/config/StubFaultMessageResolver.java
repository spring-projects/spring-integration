/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
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
