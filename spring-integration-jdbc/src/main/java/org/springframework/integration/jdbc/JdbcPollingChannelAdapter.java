/*
 * Copyright 2002-2012 the original author or authors.
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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import javax.sql.DataSource;

import org.springframework.dao.DataAccessException;
import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.integration.core.MessageSource;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.RowMapperResultSetExtractor;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.messaging.Message;

/**
 * A polling channel adapter that creates messages from the payload returned by
 * executing a select query. Optionally an update can be executed after the
 * select in order to update processed rows.
 *
 * @author Jonas Partner
 * @author Dave Syer
 * @since 2.0
 */
public class JdbcPollingChannelAdapter extends IntegrationObjectSupport implements MessageSource<Object> {

	private final NamedParameterJdbcOperations jdbcOperations;

	private final String selectQuery;

	private volatile RowMapper<?> rowMapper;

	private volatile SqlParameterSource sqlQueryParameterSource;

	private volatile boolean updatePerRow = false;

	private volatile String updateSql;

	private volatile SqlParameterSourceFactory sqlParameterSourceFactory = new ExpressionEvaluatingSqlParameterSourceFactory();

	private volatile boolean sqlParameterSourceFactorySet;

	private volatile int maxRowsPerPoll = 0;

	/**
	 * Constructor taking {@link DataSource} from which the DB Connection can be
	 * obtained and the select query to execute to retrieve new rows.
	 *
	 * @param dataSource Must not be null
	 * @param selectQuery query to execute
	 */
	public JdbcPollingChannelAdapter(DataSource dataSource, String selectQuery) {
		this.jdbcOperations = new NamedParameterJdbcTemplate(dataSource);
		this.selectQuery = selectQuery;
	}

	/**
	 * Constructor taking {@link JdbcOperations} instance to use for query
	 * execution and the select query to execute to retrieve new rows.
	 *
	 * @param jdbcOperations instance to use for query execution
	 * @param selectQuery query to execute
	 */
	public JdbcPollingChannelAdapter(JdbcOperations jdbcOperations, String selectQuery) {
		this.jdbcOperations = new NamedParameterJdbcTemplate(jdbcOperations);
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
	 *
	 * @param sqlQueryParameterSource the sql query parameter source to set
	 */
	public void setSelectSqlParameterSource(SqlParameterSource sqlQueryParameterSource) {
		this.sqlQueryParameterSource = sqlQueryParameterSource;
	}

	/**
	 * The maximum number of rows to pull out of the query results per poll (if
	 * greater than zero, otherwise all rows will be packed into the outgoing
	 * message). Default is zero.
	 *
	 * @param maxRows the max rows to set
	 */
	public void setMaxRowsPerPoll(int maxRows) {
		this.maxRowsPerPoll = maxRows;
	}

	@Override
	protected void onInit() throws Exception {
		super.onInit();
		if (!this.sqlParameterSourceFactorySet && this.getBeanFactory() != null) {
			((ExpressionEvaluatingSqlParameterSourceFactory)this.sqlParameterSourceFactory)
				.setBeanFactory(this.getBeanFactory());
		}
	}

	/**
	 * Executes the query. If a query result set contains one or more rows, the
	 * Message payload will contain either a List of Maps for each row or, if a
	 * RowMapper has been provided, the values mapped from those rows. If the
	 * query returns no rows, this method will return <code>null</code>.
	 */
	public Message<Object> receive() {
		Object payload = poll();
		if (payload == null) {
			return null;
		}
		return this.getMessageBuilderFactory().withPayload(payload).build();
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
		if (payload != null && updateSql != null) {
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

	private void executeUpdateQuery(Object obj) {
		SqlParameterSource updateParamaterSource = this.sqlParameterSourceFactory.createParameterSource(obj);
		this.jdbcOperations.update(this.updateSql, updateParamaterSource);
	}

	protected List<?> doPoll(SqlParameterSource sqlQueryParameterSource) {

		List<?> payload = null;
		final RowMapper<?> rowMapper = this.rowMapper == null ? new ColumnMapRowMapper() : this.rowMapper;
		ResultSetExtractor<List<Object>> resultSetExtractor;

		if (maxRowsPerPoll > 0) {
			resultSetExtractor = new ResultSetExtractor<List<Object>>() {
				public List<Object> extractData(ResultSet rs) throws SQLException, DataAccessException {
					List<Object> results = new ArrayList<Object>(maxRowsPerPoll);
					int rowNum = 0;
					while (rs.next() && rowNum < maxRowsPerPoll) {
						results.add(rowMapper.mapRow(rs, rowNum++));
					}
					return results;
				}
			};
		}
		else {
			@SuppressWarnings("unchecked")
			ResultSetExtractor<List<Object>> temp = new RowMapperResultSetExtractor<Object>(
					(RowMapper<Object>) rowMapper);
			resultSetExtractor = temp;
		}

		if (sqlQueryParameterSource != null) {
			payload = this.jdbcOperations.query(this.selectQuery,
					sqlQueryParameterSource, resultSetExtractor);
		}
		else {
			payload = this.jdbcOperations.getJdbcOperations().query(this.selectQuery, resultSetExtractor);
		}

		return payload;
	}
	@Override
	public String getComponentType(){
		return "jdbc:inbound-channel-adapter";
	}

}
