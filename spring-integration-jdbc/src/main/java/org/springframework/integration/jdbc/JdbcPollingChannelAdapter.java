/*
 * Copyright 2002-2021 the original author or authors.
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

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.function.Consumer;

import javax.sql.DataSource;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.endpoint.AbstractMessageSource;
import org.springframework.jdbc.core.ColumnMapRowMapper;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ParameterDisposer;
import org.springframework.jdbc.core.PreparedStatementCreator;
import org.springframework.jdbc.core.PreparedStatementCreatorFactory;
import org.springframework.jdbc.core.PreparedStatementSetter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlProvider;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.lang.Nullable;
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
public class JdbcPollingChannelAdapter extends AbstractMessageSource<Object> {

	private final NamedParameterJdbcOperations jdbcOperations;

	private RowMapper<?> rowMapper;

	private SqlParameterSource sqlQueryParameterSource;

	private boolean updatePerRow = false;

	private SqlParameterSourceFactory sqlParameterSourceFactory = new ExpressionEvaluatingSqlParameterSourceFactory();

	private boolean sqlParameterSourceFactorySet;

	private int maxRows = 0;

	private volatile String selectQuery;

	private volatile String updateSql;

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
		this.jdbcOperations = new NamedParameterJdbcTemplate(jdbcOperations) {

			@Override
			protected PreparedStatementCreator getPreparedStatementCreator(String sql,
					SqlParameterSource paramSource, @Nullable Consumer<PreparedStatementCreatorFactory> customizer) {

				PreparedStatementCreator preparedStatementCreator =
						super.getPreparedStatementCreator(sql, paramSource, customizer);

				return new PreparedStatementCreatorWithMaxRows(preparedStatementCreator,
						JdbcPollingChannelAdapter.this.maxRows);
			}

		};

		setSelectQuery(selectQuery);
		this.rowMapper = new ColumnMapRowMapper();
	}

	/**
	 * Set a {@link RowMapper}.
	 * @param rowMapper the {@link RowMapper} to use.
	 */
	public void setRowMapper(@Nullable RowMapper<?> rowMapper) {
		this.rowMapper = rowMapper;
		if (rowMapper == null) {
			this.rowMapper = new ColumnMapRowMapper();
		}
	}

	/**
	 * Set the select query.
	 * @param selectQuery the query.
	 * @since 5.2.1
	 */
	public final void setSelectQuery(String selectQuery) {
		Assert.hasText(selectQuery, "'selectQuery' must be specified.");
		this.selectQuery = selectQuery;
	}

	/**
	 * Set an update query.
	 * @param updateSql the update query to use.
	 */
	public void setUpdateSql(String updateSql) {
		this.updateSql = updateSql;
	}

	/**
	 * Set a flag to update per record or not. Defaults to false.
	 * @param updatePerRow the flag to control an update per record or whole batch.
	 */
	public void setUpdatePerRow(boolean updatePerRow) {
		this.updatePerRow = updatePerRow;
	}

	/**
	 * Set an {@link SqlParameterSourceFactory} for update query.
	 * @param sqlParameterSourceFactory the {@link SqlParameterSourceFactory} to use.
	 */
	public void setUpdateSqlParameterSourceFactory(SqlParameterSourceFactory sqlParameterSourceFactory) {
		Assert.notNull(sqlParameterSourceFactory, "'sqlParameterSourceFactory' must be null.");
		this.sqlParameterSourceFactory = sqlParameterSourceFactory;
		this.sqlParameterSourceFactorySet = true;
	}

	/**
	 * A source of parameters for the select query used for polling.
	 * @param sqlQueryParameterSource the sql query parameter source to set
	 */
	public void setSelectSqlParameterSource(@Nullable SqlParameterSource sqlQueryParameterSource) {
		this.sqlQueryParameterSource = sqlQueryParameterSource;
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
	protected void onInit() {
		BeanFactory beanFactory = getBeanFactory();
		if (!this.sqlParameterSourceFactorySet && beanFactory != null) {
			((ExpressionEvaluatingSqlParameterSourceFactory) this.sqlParameterSourceFactory)
					.setBeanFactory(beanFactory);
		}
	}

	@Override
	public String getComponentType() {
		return "jdbc:inbound-channel-adapter";
	}

	/**
	 * Execute the select query and the update query if provided. Returns the
	 * rows returned by the select query. If a RowMapper has been provided, the
	 * mapped results are returned.
	 */
	@Override
	protected Object doReceive() {
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

	/**
	 * Perform a select against provided {@link SqlParameterSource}.
	 * @param sqlQueryParameterSource the {@link SqlParameterSource} to use. Optional.
	 * @return the result of the query.
	 */
	protected List<?> doPoll(@Nullable SqlParameterSource sqlQueryParameterSource) {
		if (sqlQueryParameterSource != null) {
			return this.jdbcOperations.query(this.selectQuery, sqlQueryParameterSource, this.rowMapper);
		}
		else {
			return this.jdbcOperations.query(this.selectQuery, this.rowMapper);
		}
	}

	private void executeUpdateQuery(Object obj) {
		this.jdbcOperations.update(this.updateSql, this.sqlParameterSourceFactory.createParameterSource(obj));
	}

	private record PreparedStatementCreatorWithMaxRows(PreparedStatementCreator delegate, int maxRows)
			implements PreparedStatementCreator, PreparedStatementSetter, SqlProvider, ParameterDisposer {

		@Override
		public PreparedStatement createPreparedStatement(Connection con) throws SQLException {
			PreparedStatement preparedStatement = this.delegate.createPreparedStatement(con);
			preparedStatement.setMaxRows(this.maxRows); // We can't mutate provided JdbOperations for this option
			return preparedStatement;
		}

		@Override
		public String getSql() {
			if (this.delegate instanceof SqlProvider) {
				return ((SqlProvider) this.delegate).getSql();
			}
			else {
				return null;
			}
		}

		@Override
		public void setValues(PreparedStatement ps) throws SQLException {
			if (this.delegate instanceof PreparedStatementSetter) {
				((PreparedStatementSetter) this.delegate).setValues(ps);
			}
		}

		@Override
		public void cleanupParameters() {
			if (this.delegate instanceof ParameterDisposer) {
				((ParameterDisposer) this.delegate).cleanupParameters();
			}
		}

	}

}
