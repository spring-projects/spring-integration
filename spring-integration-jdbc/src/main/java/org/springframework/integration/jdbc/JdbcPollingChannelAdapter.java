/*
 * Copyright 2002-2018 the original author or authors.
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

import java.sql.PreparedStatement;
import java.util.List;
import java.util.function.Consumer;

import javax.sql.DataSource;

import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.integration.core.MessageSource;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.PreparedStatementCreatorFactory;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

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
 */
public class JdbcPollingChannelAdapter extends IntegrationObjectSupport implements MessageSource<Object> {

	private final NamedParameterJdbcOperations jdbcOperations;

	private final String selectQuery;

	private RowMapper<?> rowMapper;

	private SqlParameterSource sqlQueryParameterSource;

	private boolean updatePerRow = false;

	private String updateSql;

	private SqlParameterSourceFactory sqlParameterSourceFactory = new ExpressionEvaluatingSqlParameterSourceFactory();

	private boolean sqlParameterSourceFactorySet;

	private int maxRows = 0;

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
		Assert.hasText(selectQuery, "'selectQuery' must be specified.");
		this.jdbcOperations = new NamedParameterJdbcTemplate(jdbcOperations) {

			@Override
			protected PreparedStatementCreator getPreparedStatementCreator(String sql,
					SqlParameterSource paramSource, Consumer<PreparedStatementCreatorFactory> customizer) {

				PreparedStatementCreator preparedStatementCreator =
						super.getPreparedStatementCreator(sql, paramSource, customizer);

				return con -> {
					PreparedStatement preparedStatement = preparedStatementCreator.createPreparedStatement(con);
					preparedStatement.setMaxRows(JdbcPollingChannelAdapter.this.maxRows);
					return preparedStatement;
				};
			}
		};

		this.selectQuery = selectQuery;
	}

	public void setRowMapper(RowMapper<?> rowMapper) {
		this.rowMapper = rowMapper;
	}

	public void setUpdateSql(String updateSql) {
		this.updateSql = updateSql;
	}

	public void setUpdatePerRow(boolean updatePerRow) {
		this.updatePerRow = updatePerRow;
	}

	public void setUpdateSqlParameterSourceFactory(SqlParameterSourceFactory sqlParameterSourceFactory) {
		this.sqlParameterSourceFactory = sqlParameterSourceFactory;
		this.sqlParameterSourceFactorySet = true;
	}

	/**
	 * A source of parameters for the select query used for polling.
	 * @param sqlQueryParameterSource the sql query parameter source to set
	 */
	public void setSelectSqlParameterSource(SqlParameterSource sqlQueryParameterSource) {
		this.sqlQueryParameterSource = sqlQueryParameterSource;
	}

	/**
	 * The maximum number of rows to pull out of the query results per poll (if
	 * greater than zero, otherwise all rows will be packed into the outgoing
	 * message). Default is zero.
	 * @param maxRows the max rows to set
	 * @deprecated since 5.1 in favor of {@link #setMaxRows(int)}
	 */
	@Deprecated
	public void setMaxRowsPerPoll(int maxRows) {
		setMaxRows(maxRows);
	}

	/**
	 * The maximum number of rows to query. Default is zero - select all records.
	 * @param maxRows the max rows to set
	 * @since 5.1
	 */
	public void setMaxRows(int maxRows) {
		this.maxRows = maxRows;
	}

	@Override
	protected void onInit() throws Exception {
		super.onInit();
		if (!this.sqlParameterSourceFactorySet && getBeanFactory() != null) {
			((ExpressionEvaluatingSqlParameterSourceFactory) this.sqlParameterSourceFactory)
					.setBeanFactory(getBeanFactory());
		}
	}

	@Override
	public String getComponentType() {
		return "jdbc:inbound-channel-adapter";
	}

	/**
	 * Execute the query. If a query result set contains one or more rows, the
	 * Message payload will contain either a List of Maps for each row or, if a
	 * RowMapper has been provided, the values mapped from those rows. If the
	 * query returns no rows, this method will return <code>null</code>.
	 * #return the {@link Message} or {@code null} as a result of query.
	 */
	@Override
	public Message<Object> receive() {
		Object payload = poll();
		if (payload == null) {
			return null;
		}
		return getMessageBuilderFactory()
				.withPayload(payload)
				.build();
	}

	/**
	 * Execute the select query and the update query if provided. Returns the
	 * rows returned by the select query. If a RowMapper has been provided, the
	 * mapped results are returned.
	 */
	private Object poll() {
		List<?> payload = doPoll(this.sqlQueryParameterSource);
		if (payload.size() < 1) {
			payload = null;
		}
		if (payload != null && this.updateSql != null) {
			if (this.updatePerRow) {
				for (Object row : payload) {
					executeUpdateQuery(row);
				}
			}
			else {
				executeUpdateQuery(payload);
			}
		}
		return payload;
	}

	protected List<?> doPoll(SqlParameterSource sqlQueryParameterSource) {
		final RowMapper<?> rowMapper = this.rowMapper == null ? new ColumnMapRowMapper() : this.rowMapper;

		if (sqlQueryParameterSource != null) {
			return this.jdbcOperations.query(this.selectQuery, sqlQueryParameterSource, rowMapper);
		}
		else {
			return this.jdbcOperations.query(this.selectQuery, rowMapper);
		}
	}

	private void executeUpdateQuery(Object obj) {
		SqlParameterSource updateParameterSource = this.sqlParameterSourceFactory.createParameterSource(obj);
		this.jdbcOperations.update(this.updateSql, updateParameterSource);
	}

}
