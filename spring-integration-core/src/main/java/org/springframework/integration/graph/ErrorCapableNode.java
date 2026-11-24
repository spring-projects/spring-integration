/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.graph;

import org.jspecify.annotations.Nullable;

/**
 * Nodes implementing this interface are capable of emitting errors.
 *
 * @author Gary Russell
 *
 * @since 4.3
 *
 */
public interface ErrorCapableNode {

	@Nullable
	String getErrors();

}
