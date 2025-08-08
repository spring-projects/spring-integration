/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.zookeeper.metadata;

/**
 * A {@code ZookeeperMetadataStore}-specific exception.
 *
 * @author Marius Bogoevici
 * @author Artem Bilan
 *
 * @since 4.2
 */
@SuppressWarnings("serial")
public class ZookeeperMetadataStoreException extends RuntimeException {

	public ZookeeperMetadataStoreException(String message) {
		super(message);
	}

	public ZookeeperMetadataStoreException(String message, Throwable cause) {
		super(message, cause);
	}

}
