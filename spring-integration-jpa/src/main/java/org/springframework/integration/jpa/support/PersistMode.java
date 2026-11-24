/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
