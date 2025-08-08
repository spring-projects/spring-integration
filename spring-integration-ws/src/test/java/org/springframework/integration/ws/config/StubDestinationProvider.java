/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
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
