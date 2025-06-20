/*
 * Copyright 2025-present the original author or authors.
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

package org.springframework.integration.jdbc.dsl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.expression.Expression;
import org.springframework.integration.dsl.IntegrationComponentSpec;
import org.springframework.integration.jdbc.SqlParameterSourceFactory;
import org.springframework.integration.jdbc.StoredProcExecutor;
import org.springframework.integration.jdbc.storedproc.ProcedureParameter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlParameter;

/**
 * A {@link StoredProcExecutor} configurer.
 *
 * @author Jiandong Ma
 * @author Artem Bilan
 *
 * @since 7.0
 */
public class StoredProcExecutorSpec extends IntegrationComponentSpec<StoredProcExecutorSpec, StoredProcExecutor> {

	private final List<ProcedureParameter> procedureParameters = new ArrayList<>();

	private final List<SqlParameter> sqlParameters = new ArrayList<>();

	private final Map<String, RowMapper<?>> returningResultSetRowMappers = new HashMap<>();

	protected StoredProcExecutorSpec(DataSource dataSource) {
		this.target = new StoredProcExecutor(dataSource);
		this.target.setReturningResultSetRowMappers(this.returningResultSetRowMappers);
	}

	/**
	 * @param ignoreColumnMetaData the ignoreColumnMetaData
	 * @return the storedProcExecutor
	 * @see StoredProcExecutor#setIgnoreColumnMetaData(boolean)
	 */
	public StoredProcExecutorSpec ignoreColumnMetaData(boolean ignoreColumnMetaData) {
		this.target.setIgnoreColumnMetaData(ignoreColumnMetaData);
		return this;
	}

	/**
	 * Add a {@link ProcedureParameter} parameter to the target {@link StoredProcExecutor}.
	 * This method can be called several times adding a new parameter each time.
	 * @param procedureParameter the stored procedure parameter to add
	 * @return the storedProcExecutor spec
	 * @see StoredProcExecutor#setProcedureParameters(List)
	 */
	public StoredProcExecutorSpec procedureParameter(ProcedureParameter procedureParameter) {
		this.procedureParameters.add(procedureParameter);
		this.target.setProcedureParameters(this.procedureParameters);
		return this;
	}

	/**
	 * @param procedureParameters the procedureParameters
	 * @return the storedProcExecutor
	 * @see StoredProcExecutor#setProcedureParameters(List)
	 */
	public StoredProcExecutorSpec procedureParameters(List<ProcedureParameter> procedureParameters) {
		this.target.setProcedureParameters(procedureParameters);
		return this;
	}

	/**
	 * Add an {@link SqlParameter} to the target {@link StoredProcExecutor}.
	 * This method can be called several times adding a new parameter each time.
	 * @param sqlParameter the {@link SqlParameter} to add
	 * @return the storedProcExecutor
	 * @see StoredProcExecutor#setSqlParameters(List)
	 */
	public StoredProcExecutorSpec sqlParameter(SqlParameter sqlParameter) {
		this.sqlParameters.add(sqlParameter);
		this.target.setSqlParameters(this.sqlParameters);
		return this;
	}

	/**
	 * @param sqlParameters the sqlParameters
	 * @return the storedProcExecutor
	 * @see StoredProcExecutor#setSqlParameters(List)
	 */
	public StoredProcExecutorSpec sqlParameters(List<SqlParameter> sqlParameters) {
		this.target.setSqlParameters(sqlParameters);
		return this;
	}

	/**
	 * @param sqlParameterSourceFactory the sqlParameterSourceFactory
	 * @return the storedProcExecutor
	 * @see StoredProcExecutor#setSqlParameterSourceFactory(SqlParameterSourceFactory)
	 */
	public StoredProcExecutorSpec sqlParameterSourceFactory(SqlParameterSourceFactory sqlParameterSourceFactory) {
		this.target.setSqlParameterSourceFactory(sqlParameterSourceFactory);
		return this;
	}

	/**
	 * @param storedProcedureName the storedProcedureName
	 * @return the storedProcExecutor
	 * @see StoredProcExecutor#setStoredProcedureName(String)
	 */
	public StoredProcExecutorSpec storedProcedureName(String storedProcedureName) {
		this.target.setStoredProcedureName(storedProcedureName);
		return this;
	}

	/**
	 * @param storedProcedureNameExpression the storedProcedureNameExpression
	 * @return the storedProcExecutor
	 * @see StoredProcExecutor#setStoredProcedureNameExpression(Expression)
	 */
	public StoredProcExecutorSpec storedProcedureNameExpression(Expression storedProcedureNameExpression) {
		this.target.setStoredProcedureNameExpression(storedProcedureNameExpression);
		return this;
	}

	/**
	 * @param usePayloadAsParameterSource the usePayloadAsParameterSource
	 * @return the storedProcExecutor
	 * @see StoredProcExecutor#setUsePayloadAsParameterSource(boolean)
	 */
	public StoredProcExecutorSpec usePayloadAsParameterSource(boolean usePayloadAsParameterSource) {
		this.target.setUsePayloadAsParameterSource(usePayloadAsParameterSource);
		return this;
	}

	/**
	 * @param isFunction the isFunction
	 * @return the storedProcExecutor
	 * @see StoredProcExecutor#setIsFunction(boolean)
	 */
	public StoredProcExecutorSpec isFunction(boolean isFunction) {
		this.target.setIsFunction(isFunction);
		return this;
	}

	/**
	 * @param returnValueRequired the returnValueRequired
	 * @return the storedProcExecutor
	 * @see StoredProcExecutor#setReturnValueRequired(boolean)
	 */
	public StoredProcExecutorSpec returnValueRequired(boolean returnValueRequired) {
		this.target.setReturnValueRequired(returnValueRequired);
		return this;
	}

	/**
	 * @param skipUndeclaredResults the skipUndeclaredResults
	 * @return the storedProcExecutor
	 * @see StoredProcExecutor#setSkipUndeclaredResults(boolean)
	 */
	public StoredProcExecutorSpec skipUndeclaredResults(boolean skipUndeclaredResults) {
		this.target.setSkipUndeclaredResults(skipUndeclaredResults);
		return this;
	}

	/**
	 * Add a {@link RowMapper} for specific returning result from stored procedure execution.
	 * @param returningResultName the name of procedure output parameter for returning result
	 * @param rowMapper the {@link RowMapper} for returning result
	 * @return the storedProcExecutor
	 * @see StoredProcExecutor#setReturningResultSetRowMappers(Map)
	 */
	public StoredProcExecutorSpec returningResultSetRowMapper(String returningResultName, RowMapper<?> rowMapper) {
		this.returningResultSetRowMappers.put(returningResultName, rowMapper);
		return this;
	}

	/**
	 * @param returningResultSetRowMappers the returningResultSetRowMappers
	 * @return the storedProcExecutor
	 * @see StoredProcExecutor#setReturningResultSetRowMappers(Map)
	 */
	public StoredProcExecutorSpec returningResultSetRowMappers(Map<String, RowMapper<?>> returningResultSetRowMappers) {
		this.target.setReturningResultSetRowMappers(returningResultSetRowMappers);
		return this;
	}

	/**
	 * @param jdbcCallOperationsCacheSize the jdbcCallOperationsCacheSize
	 * @return the storedProcExecutor
	 * @see StoredProcExecutor#setJdbcCallOperationsCacheSize(int)
	 */
	public StoredProcExecutorSpec jdbcCallOperationsCacheSize(int jdbcCallOperationsCacheSize) {
		this.target.setJdbcCallOperationsCacheSize(jdbcCallOperationsCacheSize);
		return this;
	}

}
