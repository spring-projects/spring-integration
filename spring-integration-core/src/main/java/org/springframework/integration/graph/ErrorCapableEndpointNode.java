/*
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.graph;

import org.jspecify.annotations.Nullable;

/**
 * Represents nodes that can natively handle errors.
 *
 * @author Gary Russell
 *
 * @since 4.3
 *
 */
public class ErrorCapableEndpointNode extends EndpointNode implements ErrorCapableNode {

	private final @Nullable String errors;

	protected ErrorCapableEndpointNode(int nodeId, String name, Object nodeObject, @Nullable String output,
			@Nullable String errors) {

		super(nodeId, name, nodeObject, output);
		this.errors = errors;
	}

	@Override
	public @Nullable String getErrors() {
		return this.errors;
	}

}
