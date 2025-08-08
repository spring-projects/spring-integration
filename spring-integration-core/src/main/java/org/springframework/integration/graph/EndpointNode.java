/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.graph;

/**
 * Base class for all endpoints.
 *
 * @author Gary Russell
 *
 * @since 4.3
 *
 */
public abstract class EndpointNode extends IntegrationNode {

	private final String output;

	protected EndpointNode(int nodeId, String name, Object nodeObject, String output) {
		super(nodeId, name, nodeObject);
		this.output = output;
	}

	public String getOutput() {
		return this.output;
	}

}
