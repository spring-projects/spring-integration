/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.graph;

import org.springframework.integration.gateway.MessagingGatewaySupport;

/**
 * Represents an inbound gateway.
 *
 * @author Gary Russell
 *
 * @since 4.3
 *
 */
public class MessageGatewayNode extends ErrorCapableEndpointNode {

	public MessageGatewayNode(int nodeId, String name, MessagingGatewaySupport gateway, String output, String errors) {
		super(nodeId, name, gateway, output, errors);
	}

}
