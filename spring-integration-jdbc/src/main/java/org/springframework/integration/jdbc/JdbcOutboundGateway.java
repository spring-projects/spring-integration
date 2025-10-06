/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.jdbc;

import javax.sql.DataSource;

import org.jspecify.annotations.Nullable;

import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * @author Dave Syer
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 2.0
 *
 * @deprecated since 7.0 in favor of {@link org.springframework.integration.jdbc.outbound.JdbcOutboundGateway}
 */
@Deprecated(forRemoval = true, since = "7.0")
public class JdbcOutboundGateway extends org.springframework.integration.jdbc.outbound.JdbcOutboundGateway {

	/**
	 * Construct an instance based on the provided {@link DataSource} and update SQL.
	 * @param dataSource the {@link DataSource} for execution.
	 * @param updateQuery the query to execute.
	 */
	public JdbcOutboundGateway(DataSource dataSource, String updateQuery) {
		this(new JdbcTemplate(dataSource), updateQuery, null);
	}

	/**
	 * Construct an instance based on the provided {@link DataSource}, select and update SQLs.
	 * @param dataSource the {@link DataSource} for execution.
	 * @param updateQuery the update to execute.
	 * @param selectQuery the select to execute.
	 */
	public JdbcOutboundGateway(DataSource dataSource, String updateQuery, String selectQuery) {
		this(new JdbcTemplate(dataSource), updateQuery, selectQuery);
	}

	/**
	 * Construct an instance based on the provided {@link JdbcOperations} and update SQL.
	 * @param jdbcOperations the {@link JdbcOperations} for execution.
	 * @param updateQuery the query to execute.
	 */
	public JdbcOutboundGateway(JdbcOperations jdbcOperations, String updateQuery) {
		this(jdbcOperations, updateQuery, null);
	}

	/**
	 * Construct an instance based on the provided {@link JdbcOperations}, select and update SQLs.
	 * @param jdbcOperations the {@link JdbcOperations} for execution.
	 * @param updateQuery the update to execute.
	 * @param selectQuery the select to execute.
	 */
	public JdbcOutboundGateway(JdbcOperations jdbcOperations, String updateQuery, @Nullable String selectQuery) {
		super(jdbcOperations, updateQuery, selectQuery);
	}

}
