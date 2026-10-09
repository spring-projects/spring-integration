/*
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.graph;

import org.jspecify.annotations.Nullable;

/**
 * Base class for all endpoints.
 *
 * @author Gary Russell
 *
 * @since 4.3
 *
 */
public abstract class EndpointNode extends IntegrationNode {

	private final @Nullable String output;

	protected EndpointNode(int nodeId, String name, Object nodeObject, @Nullable String output) {
		super(nodeId, name, nodeObject);
		this.output = output;
	}

	public @Nullable String getOutput() {
		return this.output;
	}

}
