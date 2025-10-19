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

import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * A polling channel adapter that creates messages from the payload returned by
 * executing a select query. Optionally an update can be executed after the
 * select in order to update processed rows.
 *
 * @author Jonas Partner
 * @author Dave Syer
 * @author Artem Bilan
 *
 * @since 2.0
 *
 * @deprecated since 7.0 in favor of {@link org.springframework.integration.jdbc.inbound.JdbcPollingChannelAdapter}
 */
@Deprecated(forRemoval = true, since = "7.0")
public class JdbcPollingChannelAdapter extends org.springframework.integration.jdbc.inbound.JdbcPollingChannelAdapter {

	/**
	 * Constructor taking {@link DataSource} from which the DB Connection can be
	 * obtained and the select query to execute to retrieve new rows.
	 * @param dataSource Must not be null
	 * @param selectQuery query to execute
	 */
	public JdbcPollingChannelAdapter(DataSource dataSource, String selectQuery) {
		this(new JdbcTemplate(dataSource), selectQuery);
	}

	/**
	 * Constructor taking {@link JdbcOperations} instance to use for query
	 * execution and the select query to execute to retrieve new rows.
	 * @param jdbcOperations instance to use for query execution
	 * @param selectQuery query to execute
	 */
	public JdbcPollingChannelAdapter(JdbcOperations jdbcOperations, String selectQuery) {
		super(jdbcOperations, selectQuery);
	}

}
