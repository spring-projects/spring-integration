/*
 * Copyright 2002-2012 the original author or authors.
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

import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.expression.Expression;
import org.springframework.messaging.Message;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.jdbc.storedproc.ProcedureParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.jdbc.core.simple.SimpleJdbcCallOperations;
import org.springframework.util.Assert;


/**
 * A message handler that executes Stored Procedures for update purposes.
 *
 * Stored procedure parameter values are by default automatically extracted from
 * the Payload if the payload's bean properties match the parameters of the Stored
 * Procedure.
 *
 * This may be sufficient for basic use cases. For more sophisticated options
 * consider passing in one or more {@link ProcedureParameter}.
 *
 * If you need to handle the return parameters of the called stored procedure
 * explicitly, please consider using a {@link StoredProcOutboundGateway} instead.
 *
 * Also, if you need to execute SQL Functions, please also use the
 * {@link StoredProcOutboundGateway}. As functions are typically used to look up
 * values, only, the Stored Procedure message handler purposefully does not support
 * SQL function calls. If you believe there are valid use-cases for that, please file a
 * feature request at http://jira.springsource.org.
 *
 * @author Gunnar Hillert
 * @since 2.1
 */
public class StoredProcMessageHandler extends AbstractMessageHandler implements InitializingBean {

	private final StoredProcExecutor executor;

	/**
	 * Constructor taking {@link DataSource} from which the DB Connection can be
	 * obtained and the name of the stored procedure or function to
	 * execute to retrieve new rows.
	 *
	 * @param dataSource Must not be null.
	 * @param storedProcedureName The name of the Stored Procedure or Function. Must not be null.
	 *
	 * @deprecated Since 2.2 use the constructor that expects a {@link StoredProcExecutor} instead
	 */
	@Deprecated
	public StoredProcMessageHandler(DataSource dataSource, String storedProcedureName) {

		Assert.notNull(dataSource, "dataSource must not be null.");
		Assert.hasText(storedProcedureName, "storedProcedureName must not be null and cannot be empty.");

		this.executor = new StoredProcExecutor(dataSource);
		this.executor.setStoredProcedureName(storedProcedureName);

	}

	/**
	 *
	 * Constructor passing in the {@link StoredProcExecutor}.
	 *
	 * @param storedProcExecutor Must not be null.
	 *
	 */
	public StoredProcMessageHandler(StoredProcExecutor storedProcExecutor) {

		Assert.notNull(storedProcExecutor, "storedProcExecutor must not be null.");
		this.executor = storedProcExecutor;

	}

	/**
	 * Verifies parameters, sets the parameters on {@link SimpleJdbcCallOperations}
	 * and ensures the appropriate {@link SqlParameterSourceFactory} is defined
	 * when {@link ProcedureParameter} are passed in.
	 */
	@Override
	protected void onInit() throws Exception {
		super.onInit();
	};

	/**
	 * Executes the Stored procedure, delegates to executeStoredProcedure(...).
	 * Any return values from the Stored procedure are ignored.
	 *
	 * Return values are logged at debug level, though.
	 */
	@Override
	protected void handleMessageInternal(Message<?> message) {

		Map<String, Object> resultMap = executor.executeStoredProcedure(message);

		if (logger.isDebugEnabled()) {

			if (resultMap != null && !resultMap.isEmpty()) {
				logger.debug(String.format("The StoredProcMessageHandler ignores return "
					+ "values, but the called Stored Procedure returned data: '%s'", executor.getStoredProcedureName(), resultMap));
			}

		}

	}

	/**
	 * The name of the Stored Procedure or Stored Function to be executed.
	 * If {@link StoredProcExecutor#isFunction} is set to "true", then this
	 * property specifies the Stored Function name.
	 *
	 * Alternatively you can also specify the Stored Procedure name via
	 * {@link StoredProcExecutor#setStoredProcedureNameExpression(Expression)}.
	 *
	 * @param storedProcedureName Must not be null and must not be empty
	 *
	 * @see StoredProcExecutor#setStoredProcedureNameExpression(Expression)
	 *
	 * @deprecated Since 2.2 set the respective property on the passed-in {@link StoredProcExecutor}
	 *
	 * @see StoredProcExecutor#setStoredProcedureName(String)
	 */
	@Deprecated
	public void setStoredProcedureName(String storedProcedureName) {
		this.executor.setStoredProcedureName(storedProcedureName);
	}

	/**
	 * For fully supported databases, the underlying  {@link SimpleJdbcCall} can
	 * retrieve the parameter information for the to be invoked Stored Procedure
	 * from the JDBC Meta-data. However, if the used database does not support
	 * meta data lookups or if you like to provide customized parameter definitions,
	 * this flag can be set to <code>true</code>. It defaults to <code>false</code>.
	 *
	 * @deprecated Since 2.2 set the respective property on the passed-in {@link StoredProcExecutor}
	 *
	 * @see StoredProcExecutor#setIgnoreColumnMetaData(boolean)
	 */
	@Deprecated
	public void setIgnoreColumnMetaData(boolean ignoreColumnMetaData) {
		this.executor.setIgnoreColumnMetaData(ignoreColumnMetaData);
	}

	/**
	 * Custom Stored Procedure parameters that may contain static values
	 * or Strings representing an {@link Expression}.
	 *
	 * @deprecated Since 2.2 set the respective property on the passed-in {@link StoredProcExecutor}
	 *
	 * @see StoredProcExecutor#setProcedureParameters(List)
	 */
	@Deprecated
	public void setProcedureParameters(List<ProcedureParameter> procedureParameters) {
		this.executor.setProcedureParameters(procedureParameters);
	}

	/**
	 * If your database system is not fully supported by Spring and thus obtaining
	 * parameter definitions from the JDBC Meta-data is not possible, you must define
	 * the {@link SqlParameter} explicitly.
	 *
	 * @deprecated Since 2.2 set the respective property on the passed-in {@link StoredProcExecutor}
	 *
	 * @see StoredProcExecutor#setSqlParameters(List)
	 */
	@Deprecated
	public void setSqlParameters(List<SqlParameter> sqlParameters) {
		this.executor.setSqlParameters(sqlParameters);
	}

	/**
	 * Provides the ability to set a custom {@link SqlParameterSourceFactory}.
	 * Keep in mind that if {@link ProcedureParameter} are set explicitly and
	 * you would like to provide a custom {@link SqlParameterSourceFactory},
	 * then you must provide an instance of {@link ExpressionEvaluatingSqlParameterSourceFactory}.
	 *
	 * If not the SqlParameterSourceFactory will be replaced by the default
	 * {@link ExpressionEvaluatingSqlParameterSourceFactory}.
	 *
	 * @param sqlParameterSourceFactory
	 *
	 * @deprecated Since 2.2 set the respective property on the passed-in {@link StoredProcExecutor}
	 *
	 * @see StoredProcExecutor#setSqlParameterSourceFactory(SqlParameterSourceFactory)
	 */
	@Deprecated
	public void setSqlParameterSourceFactory(SqlParameterSourceFactory sqlParameterSourceFactory) {
		this.executor.setSqlParameterSourceFactory(sqlParameterSourceFactory);
	}

	/**
	 * If set to 'true', the payload of the Message will be used as a source for
	 * providing parameters. If false the entire Message will be available as a
	 * source for parameters.
	 *
	 * If no {@link ProcedureParameter} are passed in, this property will default to
	 * 'true'. This means that using a default {@link BeanPropertySqlParameterSourceFactory}
	 * the bean properties of the payload will be used as a source for parameter values for
	 * the to-be-executed Stored Procedure or Function.
	 *
	 * However, if {@link ProcedureParameter} are passed in, then this property
	 * will by default evaluate to 'false'. {@link ProcedureParameter} allow for
	 * SpEl Expressions to be provided and therefore it is highly beneficial to
	 * have access to the entire {@link Message}.
	 *
	 * @param usePayloadAsParameterSource If false the entire {@link Message} is used as parameter source.
	 *
	 * @deprecated Since 2.2 set the respective property on the passed-in {@link StoredProcExecutor}
	 *
	 * @see StoredProcExecutor#setUsePayloadAsParameterSource(boolean)
	 */
	@Deprecated
	public void setUsePayloadAsParameterSource(boolean usePayloadAsParameterSource) {
		this.executor.setUsePayloadAsParameterSource(usePayloadAsParameterSource);
	}

}
