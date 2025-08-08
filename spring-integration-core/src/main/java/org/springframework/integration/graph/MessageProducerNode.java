/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.graph;

import org.springframework.integration.endpoint.MessageProducerSupport;

/**
 * Represents an inbound message producer.
 *
 * @author Gary Russell
 *
 * @since 4.3
 *
 */
public class MessageProducerNode extends ErrorCapableEndpointNode {

	public MessageProducerNode(int nodeId, String name, MessageProducerSupport producer, String output, String errors) {
		super(nodeId, name, producer, output, errors);
	}

}
