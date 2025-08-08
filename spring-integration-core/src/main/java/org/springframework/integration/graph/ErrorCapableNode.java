/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.graph;

/**
 * Nodes implementing this interface are capable of emitting errors.
 *
 * @author Gary Russell
 *
 * @since 4.3
 *
 */
public interface ErrorCapableNode {

	String getErrors();

}
