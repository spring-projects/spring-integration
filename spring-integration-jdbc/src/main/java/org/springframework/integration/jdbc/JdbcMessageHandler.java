/*
 * Copyright 2002-2010 the original author or authors.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.integration.jdbc;

import javax.sql.DataSource;

import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageDeliveryException;
import org.springframework.integration.message.MessageHandler;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.integration.message.MessageRejectedException;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcOperations;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

/**
 * A message handler that executes an SQL update. Dynamic query parameters are supported through the
 * {@link SqlParameterSourceFactory} abstraction, the default implementation of which wraps the message so that its bean
 * properties can be referred to by name in the query string E.g.
 * 
 * <pre>
 * INSERT INTO FOOS (MESSAGE_ID, PAYLOAD) VALUES (:headers[$id], :payload)
 * </pre>
 * 
 * N.B. do not use quotes to escape the header keys. The default SQL parameter source (from Spring JDBC) can also handle
 * headers with dotted names (e.g. <code>business.id</code>)
 * 
 * @author Dave Syer
 * @since 2.0
 */
public class JdbcMessageHandler implements MessageHandler {

	private final SimpleJdbcOperations jdbcOperations;

	private volatile String updateSql;

	private volatile SqlParameterSourceFactory sqlParameterSourceFactory = new DefaultSqlParameterSourceFactory();

	/**
	 * Constructor taking {@link DataSource} from which the DB Connection can be obtained and the select query to
	 * execute to retrieve new rows.
	 * 
	 * @param dataSource used to create a {@link SimpleJdbcTemplate}
	 * @param updateSql query to execute
	 */
	public JdbcMessageHandler(DataSource dataSource, String updateSql) {
		this.jdbcOperations = new SimpleJdbcTemplate(dataSource);
		this.updateSql = updateSql;
	}

	/**
	 * Constructor taking {@link JdbcOperations} instance to use for query execution and the select query to execute to
	 * retrieve new rows.
	 * 
	 * @param jdbcOperations instance to use for query execution
	 * @param updateSql query to execute
	 */
	public JdbcMessageHandler(JdbcOperations jdbcOperations, String updateSql) {
		this.jdbcOperations = new SimpleJdbcTemplate(jdbcOperations);
		this.updateSql = updateSql;
	}

	public void setUpdateSql(String updateSql) {
		this.updateSql = updateSql;
	}

	public void setSqlParameterSourceFactory(SqlParameterSourceFactory sqlParameterSourceFactory) {
		this.sqlParameterSourceFactory = sqlParameterSourceFactory;
	}

	/**
	 * Executes the update, passing the message into the {@link SqlParameterSourceFactory}.
	 */
	public void handleMessage(Message<?> message) throws MessageRejectedException, MessageHandlingException,
			MessageDeliveryException {
		executeUpdateQuery(message);
	}

	private void executeUpdateQuery(Object obj) {
		SqlParameterSource updateParamaterSource = null;
		if (this.sqlParameterSourceFactory != null) {
			updateParamaterSource = this.sqlParameterSourceFactory.createParameterSource(obj);
			this.jdbcOperations.update(this.updateSql, updateParamaterSource);
		} else {
			this.jdbcOperations.update(this.updateSql);
		}
	}

}
