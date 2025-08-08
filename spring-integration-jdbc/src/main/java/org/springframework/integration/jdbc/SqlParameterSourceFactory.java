/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jdbc;

import org.springframework.jdbc.core.namedparam.SqlParameterSource;

/**
 * Collaborator for JDBC adapters which allows creation of
 * instances of {@link SqlParameterSource} for use in update operations.
 *
 * @author Jonas Partner
 * @since 2.0
 */
@FunctionalInterface
public interface SqlParameterSourceFactory {

	/**
	 * Return a new {@link SqlParameterSource}.
	 *
	 * @param input the raw message or query result to be transformed into a SqlParameterSource
	 * @return The parameter source.
	 */
	SqlParameterSource createParameterSource(Object input);

}
