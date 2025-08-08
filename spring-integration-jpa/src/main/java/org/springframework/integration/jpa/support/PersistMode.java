/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jpa.support;

/**
 * Indicates how entities shall be persisted to the underlying persistence store.
 *
 * @author Gunnar Hillert
 * @since 2.2
 *
 */
public enum PersistMode {
	PERSIST, MERGE, DELETE
}
