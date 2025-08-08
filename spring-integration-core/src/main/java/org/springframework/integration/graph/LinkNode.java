/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.graph;

/**
 * Represents a link between nodes.
 *
 * @author Gary Russell
 * @since 4.3
 *
 */
public class LinkNode {

	private final int from;

	private final int to;

	private final Type type;

	public LinkNode(int from, int to, Type type) {
		this.from = from;
		this.to = to;
		this.type = type;
	}

	public int getFrom() {
		return this.from;
	}

	public int getTo() {
		return this.to;
	}

	public Type getType() {
		return this.type;
	}

	public enum Type {
		input, output, error, discard, route
	}

}
