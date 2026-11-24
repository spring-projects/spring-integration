/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
