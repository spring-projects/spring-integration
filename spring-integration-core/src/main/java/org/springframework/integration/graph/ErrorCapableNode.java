/*
 * Copyright 2016-present the original author or authors.
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
