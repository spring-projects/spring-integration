/*
 * Copyright 2002-2008 the original author or authors.
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

import java.util.List;

import javax.sql.DataSource;

import org.springframework.integration.core.Message;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.message.MessageSource;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcOperations;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;

/**
 * A polling channel adapter that creates messages from the payload returned by
 * executing a select query Optionally an update can be executed after the
 * select in order to update processed rows
 * 
 * @author Jonas Partner
 */
public class JdbcPollingChannelAdapter implements MessageSource<Object> {

	private final SimpleJdbcOperations jdbcOperations;

	private final String selectQuery;

	private volatile RowMapper<?> rowMapper;

	private volatile SqlParameterSource sqlQueryParameterSource;

	private volatile boolean updatePerRow = false;

	private volatile String updateSql;

	private volatile SqlParamterSourceFactory sqlParameterSourceFactoryForUpdate = new DefaultSqlParamterSourceFactory();

	/**
	 * Constructor taking query to execute to retreive new rows and
	 * {@link DataSource} from which the DB Connection can be obtained
	 * 
	 * @param dataSource
	 *            used to create a {@link SimpleJdbcTemplate}
	 * @param selectQuery
	 *            query to execute
	 */
	public JdbcPollingChannelAdapter(DataSource dataSource, String selectQuery) {
		this.jdbcOperations = new SimpleJdbcTemplate(dataSource);
		this.selectQuery = selectQuery;
	}

	/**
	 * Constructor taking query to execute on a poll and
	 * {@link SimpleJdbcOperations} instance to use for query execution
	 * 
	 * @param jdbcOperations
	 * @param selectQuery
	 *            query to execute
	 */
	public JdbcPollingChannelAdapter(SimpleJdbcOperations jdbcOperations,
			String selectQuery) {
		this.jdbcOperations = jdbcOperations;
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

	public void setSqlParameterSourceFactoryForUpdate(
			SqlParamterSourceFactory sqlParameterSourceFactoryForUpdate) {
		this.sqlParameterSourceFactoryForUpdate = sqlParameterSourceFactoryForUpdate;
	}

	/**
	 * Polls for new rows returning a message containing one or more rows where
	 * rows are found and null where no rows are returned by the select query
	 */
	public Message<Object> receive() {
		Object payload = null;
		payload = pollAndUpdate();
		if (payload == null) {
			return null;
		}
		return MessageBuilder.withPayload(payload).build();
	}

	/**
	 * Execute the select query and the update query if provided and rows are
	 * returned by the select query
	 * 
	 * @return
	 */
	protected Object pollAndUpdate() {
		List payload;
		if (this.rowMapper != null) {
			payload = pollWithRowMapper();
		} else {
			payload = this.jdbcOperations.queryForList(this.selectQuery,
					this.sqlQueryParameterSource);
		}

		if (payload.size() < 1) {
			payload = null;
		}

		if (payload != null && updateSql != null) {
			if (this.updatePerRow) {
				for (Object row : payload) {
					executeUpdateQuery(row);
				}
			} else {
				executeUpdateQuery(payload);
			}
		}
		return payload;

	}

	protected void executeUpdateQuery(Object obj) {
		SqlParameterSource updateParamaterSource = null;
		if (this.sqlParameterSourceFactoryForUpdate != null) {

			updateParamaterSource = this.sqlParameterSourceFactoryForUpdate
					.createParamterSource(obj);
			this.jdbcOperations.update(this.updateSql, updateParamaterSource);
		} else {
			this.jdbcOperations.update(this.updateSql);
		}
	}

	protected List pollWithRowMapper() {
		List payload = null;
		if (this.sqlQueryParameterSource != null) {
			payload = this.jdbcOperations.query(this.selectQuery,
					this.rowMapper, this.sqlQueryParameterSource);
		} else {
			payload = this.jdbcOperations.query(this.selectQuery,
					this.rowMapper);
		}
		return payload;
	}

}