/*
 * Copyright 2025 the original author or authors.
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

import java.util.List;
import java.util.Map;

import org.springframework.expression.Expression;
import org.springframework.integration.jdbc.SqlParameterSourceFactory;
import org.springframework.integration.jdbc.StoredProcExecutor;
import org.springframework.integration.jdbc.storedproc.ProcedureParameter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlParameter;

/**
 * A {@link StoredProcExecutor} configurer.
 *
 * @author Jiandong Ma
 *
 * @since 7.0
 */
public class StoredProcExecutorConfigurer {

	private final StoredProcExecutor storedProcExecutor;

	protected StoredProcExecutorConfigurer(StoredProcExecutor storedProcExecutor) {
		this.storedProcExecutor = storedProcExecutor;
	}

	/**
	 * @param ignoreColumnMetaData the ignoreColumnMetaData
	 * @return the storedProcExecutor
	 * @see StoredProcExecutor#setIgnoreColumnMetaData(boolean)
	 */
	public StoredProcExecutorConfigurer ignoreColumnMetaData(boolean ignoreColumnMetaData) {
		this.storedProcExecutor.setIgnoreColumnMetaData(ignoreColumnMetaData);
		return this;
	}

	/**
	 * @param procedureParameters the procedureParameters
	 * @return the storedProcExecutor
	 * @see StoredProcExecutor#setProcedureParameters(List)
	 */
	public StoredProcExecutorConfigurer procedureParameters(List<ProcedureParameter> procedureParameters) {
		this.storedProcExecutor.setProcedureParameters(procedureParameters);
		return this;
	}

	/**
	 * @param sqlParameters the sqlParameters
	 * @return the storedProcExecutor
	 * @see StoredProcExecutor#setSqlParameters(List)
	 */
	public StoredProcExecutorConfigurer sqlParameters(List<SqlParameter> sqlParameters) {
		this.storedProcExecutor.setSqlParameters(sqlParameters);
		return this;
	}

	/**
	 * @param sqlParameterSourceFactory the sqlParameterSourceFactory
	 * @return the storedProcExecutor
	 * @see StoredProcExecutor#setSqlParameterSourceFactory(SqlParameterSourceFactory)
	 */
	public StoredProcExecutorConfigurer sqlParameterSourceFactory(SqlParameterSourceFactory sqlParameterSourceFactory) {
		this.storedProcExecutor.setSqlParameterSourceFactory(sqlParameterSourceFactory);
		return this;
	}

	/**
	 * @param storedProcedureName the storedProcedureName
	 * @return the storedProcExecutor
	 * @see StoredProcExecutor#setStoredProcedureName(String)
	 */
	public StoredProcExecutorConfigurer storedProcedureName(String storedProcedureName) {
		this.storedProcExecutor.setStoredProcedureName(storedProcedureName);
		return this;
	}

	/**
	 * @param storedProcedureNameExpression the storedProcedureNameExpression
	 * @return the storedProcExecutor
	 * @see StoredProcExecutor#setStoredProcedureNameExpression(Expression)
	 */
	public StoredProcExecutorConfigurer storedProcedureNameExpression(Expression storedProcedureNameExpression) {
		this.storedProcExecutor.setStoredProcedureNameExpression(storedProcedureNameExpression);
		return this;
	}

	/**
	 * @param usePayloadAsParameterSource the usePayloadAsParameterSource
	 * @return the storedProcExecutor
	 * @see StoredProcExecutor#setUsePayloadAsParameterSource(boolean)
	 */
	public StoredProcExecutorConfigurer usePayloadAsParameterSource(boolean usePayloadAsParameterSource) {
		this.storedProcExecutor.setUsePayloadAsParameterSource(usePayloadAsParameterSource);
		return this;
	}

	/**
	 * @param isFunction the isFunction
	 * @return the storedProcExecutor
	 * @see StoredProcExecutor#setIsFunction(boolean)
	 */
	public StoredProcExecutorConfigurer isFunction(boolean isFunction) {
		this.storedProcExecutor.setIsFunction(isFunction);
		return this;
	}

	/**
	 * @param returnValueRequired the returnValueRequired
	 * @return the storedProcExecutor
	 * @see StoredProcExecutor#setReturnValueRequired(boolean)
	 */
	public StoredProcExecutorConfigurer returnValueRequired(boolean returnValueRequired) {
		this.storedProcExecutor.setReturnValueRequired(returnValueRequired);
		return this;
	}

	/**
	 * @param skipUndeclaredResults the skipUndeclaredResults
	 * @return the storedProcExecutor
	 * @see StoredProcExecutor#setSkipUndeclaredResults(boolean)
	 */
	public StoredProcExecutorConfigurer skipUndeclaredResults(boolean skipUndeclaredResults) {
		this.storedProcExecutor.setSkipUndeclaredResults(skipUndeclaredResults);
		return this;
	}

	/**
	 * @param returningResultSetRowMappers the returningResultSetRowMappers
	 * @return the storedProcExecutor
	 * @see StoredProcExecutor#setReturningResultSetRowMappers(Map)
	 */
	public StoredProcExecutorConfigurer returningResultSetRowMappers(Map<String, RowMapper<?>> returningResultSetRowMappers) {
		this.storedProcExecutor.setReturningResultSetRowMappers(returningResultSetRowMappers);
		return this;
	}

	/**
	 * @param jdbcCallOperationsCacheSize the jdbcCallOperationsCacheSize
	 * @return the storedProcExecutor
	 * @see StoredProcExecutor#setJdbcCallOperationsCacheSize(int)
	 */
	public StoredProcExecutorConfigurer jdbcCallOperationsCacheSize(int jdbcCallOperationsCacheSize) {
		this.storedProcExecutor.setJdbcCallOperationsCacheSize(jdbcCallOperationsCacheSize);
		return this;
	}
}
