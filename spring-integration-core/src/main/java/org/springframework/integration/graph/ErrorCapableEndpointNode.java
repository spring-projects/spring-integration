/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.graph;

/**
 * Represents nodes that can natively handle errors.
 *
 * @author Gary Russell
 *
 * @since 4.3
 *
 */
public class ErrorCapableEndpointNode extends EndpointNode implements ErrorCapableNode {

	private final String errors;

	protected ErrorCapableEndpointNode(int nodeId, String name, Object nodeObject, String output, String errors) {
		super(nodeId, name, nodeObject, output);
		this.errors = errors;
	}

	@Override
	public String getErrors() {
		return this.errors;
	}

}
