/*
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.graph;

/**
 * Represents a link between nodes.
 *
 * @param from the source node index
 * @param to the target node index
 * @param type the {@link Type} of this link
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.3
 *
 */
public record LinkNode(int from, int to, Type type) {

	public enum Type {
		input, output, error, discard, route
	}

}
