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
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.messaging.Message;

/**
 * A message handler that executes an SQL update. Dynamic query parameters are supported through the
 * {@link SqlParameterSourceFactory} abstraction, the default implementation of which wraps the message so that its bean
 * properties can be referred to by name in the query string, e.g.
 *
 * <pre class="code">
 * INSERT INTO ITEMS (MESSAGE_ID, PAYLOAD) VALUES (:headers[id], :payload)
 * </pre>
 *
 * <p>
 * When a message payload is an instance of {@link Iterable}, a
 * {@link NamedParameterJdbcOperations#batchUpdate(String, SqlParameterSource[])} is performed, where each
 * {@link SqlParameterSource} instance is based on items wrapped into an internal {@link Message} implementation with
 * headers from the request message. The item is wrapped only if it is not a {@link Message} already.
 * <p>
 * N.B. do not use quotes to escape the header keys. The default SQL parameter source (from Spring JDBC) can also handle
 * headers with dotted names (e.g. <code>business.id</code>)
 *
 * @author Dave Syer
 * @author Artem Bilan
 * @author Trung Pham
 *
 * @since 2.0
 *
 * @deprecated since 7.0 in favor of {@link org.springframework.integration.jdbc.outbound.JdbcMessageHandler}
 */
@Deprecated(forRemoval = true, since = "7.0")
public class JdbcMessageHandler extends org.springframework.integration.jdbc.outbound.JdbcMessageHandler {

	/**
	 * Constructor taking {@link DataSource} from which the DB Connection can be obtained and the select query to
	 * execute to retrieve new rows.
	 * @param dataSource Must not be null
	 * @param updateSql query to execute
	 */
	public JdbcMessageHandler(DataSource dataSource, String updateSql) {
		this(new JdbcTemplate(dataSource), updateSql);
	}

	/**
	 * Constructor taking {@link JdbcOperations} instance to use for query execution and the select query to execute to
	 * retrieve new rows.
	 * @param jdbcOperations instance to use for query execution
	 * @param updateSql query to execute
	 */
	public JdbcMessageHandler(JdbcOperations jdbcOperations, String updateSql) {
		super(jdbcOperations, updateSql);
	}

}
