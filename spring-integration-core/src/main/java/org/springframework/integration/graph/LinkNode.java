/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
