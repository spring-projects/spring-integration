/*
 * Copyright 2002-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.jdbc;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.PreparedStatementCallback;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapperResultSetExtractor;
import org.springframework.jdbc.core.namedparam.EmptySqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.JdbcUtils;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;
import org.springframework.util.LinkedCaseInsensitiveMap;

/**
 * A message handler that executes an SQL update. Dynamic query parameters are supported through the
 * {@link SqlParameterSourceFactory} abstraction, the default implementation of which wraps the message so that its bean
 * properties can be referred to by name in the query string E.g.
 *
 * <pre class="code">
 * INSERT INTO FOOS (MESSAGE_ID, PAYLOAD) VALUES (:headers[id], :payload)
 * </pre>
 *
 * N.B. do not use quotes to escape the header keys. The default SQL parameter source (from Spring JDBC) can also handle
 * headers with dotted names (e.g. <code>business.id</code>)
 *
 * @author Dave Syer
 * @author Artem Bilan
 * @since 2.0
 */
public class JdbcMessageHandler extends AbstractMessageHandler {

	private final ResultSetExtractor<List<Map<String, Object>>> generatedKeysResultSetExtractor =
			new RowMapperResultSetExtractor<Map<String, Object>>(new ColumnMapRowMapper(), 1);

	private final NamedParameterJdbcOperations jdbcOperations;

	private final PreparedStatementCreator generatedKeysStatementCreator = new PreparedStatementCreator() {

		@Override
		public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
			return con.prepareStatement(JdbcMessageHandler.this.updateSql, Statement.RETURN_GENERATED_KEYS);
		}

	};

	private volatile String updateSql;

	private volatile SqlParameterSourceFactory sqlParameterSourceFactory;

	private volatile boolean keysGenerated;

	private MessagePreparedStatementSetter preparedStatementSetter;

	/**
	 * Constructor taking {@link DataSource} from which the DB Connection can be obtained and the select query to
	 * execute to retrieve new rows.
	 *
	 * @param dataSource Must not be null
	 * @param updateSql query to execute
	 */
	public JdbcMessageHandler(DataSource dataSource, String updateSql) {
		this.jdbcOperations = new NamedParameterJdbcTemplate(dataSource);
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
		this.jdbcOperations = new NamedParameterJdbcTemplate(jdbcOperations);
		this.updateSql = updateSql;
	}

	/**
	 * Flag to indicate that the update query is an insert with auto-generated keys,
	 * which will be logged at debug level.
	 * @param keysGenerated the flag value to set
	 */
	public void setKeysGenerated(boolean keysGenerated) {
		this.keysGenerated = keysGenerated;
	}

	public void setUpdateSql(String updateSql) {
		this.updateSql = updateSql;
	}

	public void setSqlParameterSourceFactory(SqlParameterSourceFactory sqlParameterSourceFactory) {
		this.sqlParameterSourceFactory = sqlParameterSourceFactory;
	}

	/**
	 * Specify a {@link MessagePreparedStatementSetter} to populate parameters on the
	 * {@link PreparedStatement} with the {@link Message} context.
	 * <p>This is a low-level alternative to the {@link SqlParameterSourceFactory}.
	 * @param preparedStatementSetter the {@link MessagePreparedStatementSetter} to set.
	 * @since 4.2
	 */
	public void setPreparedStatementSetter(MessagePreparedStatementSetter preparedStatementSetter) {
		this.preparedStatementSetter = preparedStatementSetter;
	}

	@Override
	public String getComponentType() {
		return "jdbc:outbound-channel-adapter";
	}

	@Override
	protected void onInit() throws Exception {
		super.onInit();
		Assert.state(!(this.sqlParameterSourceFactory != null && this.preparedStatementSetter != null),
				"'sqlParameterSourceFactory' and 'preparedStatementSetter' are mutually exclusive.");
		if (this.sqlParameterSourceFactory == null && this.preparedStatementSetter == null) {
			this.sqlParameterSourceFactory = new BeanPropertySqlParameterSourceFactory();
		}
	}

	/**
	 * Executes the update, passing the message into the {@link SqlParameterSourceFactory}.
	 */
	@Override
	protected void handleMessageInternal(Message<?> message) throws Exception {
		List<? extends Map<String, Object>> keys = executeUpdateQuery(message, this.keysGenerated);
		if (!keys.isEmpty() && logger.isDebugEnabled()) {
			logger.debug("Generated keys: " + keys);
		}
	}

	protected List<? extends Map<String, Object>> executeUpdateQuery(final Message<?> message, boolean keysGenerated) {
		SqlParameterSource updateParameterSource = EmptySqlParameterSource.INSTANCE;
		if (this.preparedStatementSetter == null) {
			if (this.sqlParameterSourceFactory != null) {
				updateParameterSource = this.sqlParameterSourceFactory.createParameterSource(message);
			}
		}
		if (keysGenerated) {
			if (this.preparedStatementSetter != null) {
				return this.jdbcOperations.getJdbcOperations().execute(this.generatedKeysStatementCreator,
						new PreparedStatementCallback<List<Map<String, Object>>>() {

							@Override
							public List<Map<String, Object>> doInPreparedStatement(PreparedStatement ps)
									throws SQLException {
								JdbcMessageHandler.this.preparedStatementSetter.setValues(ps, message);
								ps.executeUpdate();
								ResultSet keys = ps.getGeneratedKeys();
								if (keys != null) {
									try {

										return JdbcMessageHandler.this.generatedKeysResultSetExtractor.extractData(keys);
									}
									finally {
										JdbcUtils.closeResultSet(keys);
									}
								}
								return new LinkedList<Map<String, Object>>();
							}

						});
			}
			else {
				KeyHolder keyHolder = new GeneratedKeyHolder();
				this.jdbcOperations.update(this.updateSql, updateParameterSource, keyHolder);
				return keyHolder.getKeyList();
			}
		}
		else {
			int updated;
			if (this.preparedStatementSetter != null) {
				updated = this.jdbcOperations.getJdbcOperations().update(this.updateSql,
						new PreparedStatementSetter() {

							@Override
							public void setValues(PreparedStatement ps) throws SQLException {
								JdbcMessageHandler.this.preparedStatementSetter.setValues(ps, message);
							}

						});
			}
			else {
				updated = this.jdbcOperations.update(this.updateSql, updateParameterSource);
			}
			LinkedCaseInsensitiveMap<Object> map = new LinkedCaseInsensitiveMap<Object>();
			map.put("UPDATED", updated);
			return Collections.singletonList(map);
		}
	}

}
