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

import java.sql.CallableStatement;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.expression.Expression;
import org.springframework.integration.Message;
import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.MessageHeaders;
import org.springframework.integration.MessagingException;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.jdbc.storedproc.ProcedureParameter;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.jdbc.core.simple.SimpleJdbcCallOperations;
import org.springframework.util.Assert;

/**
 * @author Gunnar Hillert
 *
 * @since 2.1
 */
public class StoredProcOutboundGateway extends AbstractReplyProducingMessageHandler {

	private final StoredProcExecutor executor;

	private volatile boolean expectSingleResult = false;

	/**
	 * Constructor taking {@link DataSource} from which the DB Connection can be
	 * obtained and the name of the stored procedure or function to
	 * execute to retrieve new rows.
	 *
	 * @param dataSource used to create a {@link SimpleJdbcCall} instance
	 * @param storedProcedureName Must not be null.
	 *
	 * @deprecated Since 2.2 use the constructor that expects a {@link StoredProcExecutor} instead
	 */
	@Deprecated
	public StoredProcOutboundGateway(DataSource dataSource, String storedProcedureName) {

		Assert.notNull(dataSource, "dataSource must not be null.");
		Assert.hasText(storedProcedureName, "storedProcedureName must not be null and cannot be empty.");

		this.executor = new StoredProcExecutor(dataSource);
		this.executor.setStoredProcedureName(storedProcedureName);

	}

	/**
	 * Constructor taking {@link StoredProcExecutor}.
	 *
	 * @param storedProcExecutor Must not be null.
	 *
	 */
	public StoredProcOutboundGateway(StoredProcExecutor storedProcExecutor) {

		Assert.notNull(storedProcExecutor, "storedProcExecutor must not be null.");
		this.executor = storedProcExecutor;

	}

	/**
	 * Verifies parameters, sets the parameters on {@link SimpleJdbcCallOperations}
	 * and ensures the appropriate {@link SqlParameterSourceFactory} is defined
	 * when {@link ProcedureParameter} are passed in.
	 */
	@Override
	protected void onInit() {
		super.onInit();
	};

	@Override
	protected Object handleRequestMessage(Message<?> requestMessage) {

		Map<String, Object> resultMap = executor.executeStoredProcedure(requestMessage);

		final Object payload;

		if (resultMap.isEmpty()) {
			return null;
		}
		else {

			if (this.expectSingleResult && resultMap.size() == 1) {
				payload = resultMap.values().iterator().next();
			}
			else if (this.expectSingleResult && resultMap.size() > 1) {

				throw new MessageHandlingException(requestMessage,
						"Stored Procedure/Function call returned more than "
					  + "1 result object and expectSingleResult was 'true'. ");

			}
			else {
				payload = resultMap;
			}

		}

		return MessageBuilder.withPayload(payload).copyHeaders(requestMessage.getHeaders()).build();

	}

	/**
	 * The name of the Stored Procedure or Stored Function to be executed.
	 * If {@link StoredProcExecutor#isFunction} is set to "true", then this
	 * property specifies the Stored Function name.
	 *
	 * Alternatively you can also specify the Stored Procedure name via
	 * {@link StoredProcExecutor#setStoredProcedureNameExpression(Expression)} or
	 * through {@link MessageHeaders} by enabling
	 * StoredProcExecutor#setAllowDynamicStoredProcedureNames(boolean)
	 *
	 * @param storedProcedureName Must not be null and must not be empty
	 *
	 * @see StoredProcExecutor#setStoredProcedureNameExpression(Expression)
	 * @see StoredProcExecutor#setAllowDynamicStoredProcedureNames(boolean)
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
	 * Explicit declarations are necessary if the database you use is not a
	 * Spring-supported database. Currently Spring supports metadata lookup of
	 * stored procedure calls for the following databases:
	 *
	 * <ul>
	 *  <li>Apache Derby</li>
	 *  <li>DB2</li>
	 *  <li>MySQL</li>
	 *  <li>Microsoft SQL Server</li>
	 *  <li>Oracle</li>
	 *  <li>Sybase</li>
	 *  <li>PostgreSQL</li>
	 * </ul>
	 * , ,
	 * We also support metadata lookup of stored functions for the following
	 * databases:
	 *
	 * <ul>
	 *  <li>MySQL</li>
	 *  <li>Microsoft SQL Server</li>
	 *  <li>Oracle</li>
	 *  <li>PostgreSQL</li>
	 * </ul>
	 *
	 * See also: http://static.springsource.org/spring/docs/3.1.0.M2/spring-framework-reference/html/jdbc.html
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
	 * Does your stored procedure return one or more result sets? If so, you
	 * can use the provided method for setting the respective RowMappers.
	 *
	 * @deprecated Since 2.2 set the respective property on the passed-in {@link StoredProcExecutor}
	 *
	 * @see StoredProcExecutor#setReturningResultSetRowMappers(Map)
	 */
	@Deprecated
	public void setReturningResultSetRowMappers(
			Map<String, RowMapper<?>> returningResultSetRowMappers) {
		this.executor.setReturningResultSetRowMappers(returningResultSetRowMappers);
	}

	/**
	 * If true, the JDBC parameter definitions for the stored procedure are not
	 * automatically derived from the underlying JDBC connection. In that case
	 * you must pass in {@link SqlParameter} explicitly..
	 *
	 * @param ignoreColumnMetaData Defaults to <code>false</code>.
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
	 * Indicates the procedure's return value should be included in the results
	 * returned.
	 *
	 * @param returnValueRequired
	 *
	 * @deprecated Since 2.2 set the respective property on the passed-in {@link StoredProcExecutor}
	 *
	 * @see StoredProcExecutor#setReturnValueRequired(boolean)
	 */
	@Deprecated
	public void setReturnValueRequired(boolean returnValueRequired) {
		this.executor.setReturnValueRequired(returnValueRequired);
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
	 * Indicates whether a Stored Procedure or a Function is being executed.
	 * The default value is false.
	 *
	 * @param isFunction If set to true an Sql Function is executed rather than a Stored Procedure.
	 *
	 * @deprecated Since 2.2 set the respective property on the passed-in {@link StoredProcExecutor}
	 *
	 * @see StoredProcExecutor#setIsFunction(boolean)
	 */
	@Deprecated
	public void setIsFunction(boolean isFunction) {
		this.executor.setIsFunction(isFunction);
	}

	/**
	 * This parameter indicates that only one result object shall be returned from
	 * the Stored Procedure/Function Call. If set to true, a resultMap that contains
	 * only 1 element, will have that 1 element extracted and returned as payload.
	 *
	 * If the resultMap contains more than 1 element and expectSingleResult is true,
	 * then a {@link MessagingException} is thrown.
	 *
	 * Otherwise the complete resultMap is returned as the {@link Message} payload.
	 *
	 * Important Note: Several databases such as H2 are not fully supported.
	 * The H2 database, for example, does not fully support the {@link CallableStatement}
	 * semantics and when executing function calls against H2, a result list is
	 * returned rather than a single value.
	 *
	 * Therefore, even if you set expectSingleResult = true, you may end up with
	 * a collection being returned.
	 *
	 * @param expectSingleResult
	 */
	public void setExpectSingleResult(boolean expectSingleResult) {
		this.expectSingleResult = expectSingleResult;
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

	/**
	 * If this variable is set to <code>true</code> then all results from a stored
	 * procedure call that don't have a corresponding {@link SqlOutParameter}
	 * declaration will be bypassed.
	 *
	 * E.g. Stored Procedures may return an update count value, even though your
	 * Stored Procedure only declared a single result parameter. The exact behavior
	 * depends on the used database.
	 *
	 * The value is set on the underlying {@link JdbcTemplate}.
	 *
	 * Only few developers will probably ever like to process update counts, thus
	 * the value defaults to <code>true</code>.
	 *
	 * @deprecated Since 2.2 set the respective property on the passed-in {@link StoredProcExecutor}
	 *
	 * @see StoredProcExecutor#setSkipUndeclaredResults(boolean)
	 */
	@Deprecated
	public void setSkipUndeclaredResults(boolean skipUndeclaredResults) {
		this.executor.setSkipUndeclaredResults(skipUndeclaredResults);
	}

}
