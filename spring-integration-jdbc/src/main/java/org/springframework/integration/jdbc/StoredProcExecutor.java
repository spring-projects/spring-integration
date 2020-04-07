/*
 * Copyright 2002-2020 the original author or authors.
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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.sql.DataSource;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.jdbc.storedproc.ProcedureParameter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.jdbc.core.simple.SimpleJdbcCallOperations;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;


/**
 * This class is used by all Stored Procedure (Stored Function) components and
 * provides the core functionality to execute those.
 *
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 2.1
 *
 */
public class StoredProcExecutor implements BeanFactoryAware, InitializingBean {

	private static final int DEFAULT_CACHE_SIZE = 10;

	private static final float LOAD_FACTOR = 0.75f;

	private final DataSource dataSource;

	private final Object jdbcCallOperationsMapMonitor = new Object();

	private Map<String, RowMapper<?>> returningResultSetRowMappers = new HashMap<>(0);

	private EvaluationContext evaluationContext;

	private BeanFactory beanFactory;

	private int jdbcCallOperationsCacheSize = DEFAULT_CACHE_SIZE;

	private Map<String, SimpleJdbcCallOperations> jdbcCallOperationsMap;

	private Expression storedProcedureNameExpression;

	/**
	 * For fully supported databases, the underlying  {@link SimpleJdbcCall} can
	 * retrieve the parameter information for the to be invoked Stored Procedure
	 * or Function from the JDBC Meta-data. However, if the used database does
	 * not support meta data lookups or if you like to provide customized
	 * parameter definitions, this flag can be set to 'true'. It defaults to 'false'.
	 */
	private boolean ignoreColumnMetaData = false;

	/**
	 * If this variable is set to true then all results from a stored procedure call
	 * that don't have a corresponding SqlOutParameter declaration will be bypassed.
	 * The value is set on the underlying {@link org.springframework.jdbc.core.JdbcTemplate}.
	 * Value defaults to <code>true</code>.
	 */
	private boolean skipUndeclaredResults = true;

	/**
	 * If your database system is not fully supported by Spring and thus obtaining
	 * parameter definitions from the JDBC Meta-data is not possible, you must define
	 * the {@link SqlParameter} explicitly. See also
	 * {@link org.springframework.jdbc.core.SqlOutParameter} and
	 * {@link org.springframework.jdbc.core.SqlInOutParameter}.
	 */
	private List<SqlParameter> sqlParameters = new ArrayList<>(0);

	/**
	 * By default bean properties of the passed in {@link Message} will be used
	 * as a source for the Stored Procedure's input parameters. By default a
	 * {@link BeanPropertySqlParameterSourceFactory} will be used.
	 * This may be sufficient for basic use cases. For more sophisticated options
	 * consider passing in one or more {@link ProcedureParameter}.
	 */
	private SqlParameterSourceFactory sqlParameterSourceFactory;

	/**
	 * Indicates that whether only the payload of the passed-in {@link Message}
	 * shall be used as a source of parameters.
	 * @see #setUsePayloadAsParameterSource(boolean)
	 */
	private Boolean usePayloadAsParameterSource;

	/**
	 * Custom Stored Procedure parameters that may contain static values
	 * or Strings representing an {@link Expression}.
	 */
	private List<ProcedureParameter> procedureParameters;

	private boolean isFunction = false;

	private boolean returnValueRequired = false;


	/**
	 * Constructor taking {@link DataSource} from which the DB Connection can be
	 * obtained.
	 * @param dataSource used to create a {@link SimpleJdbcCall} instance, must not be Null
	 */
	public StoredProcExecutor(DataSource dataSource) {
		Assert.notNull(dataSource, "dataSource must not be null.");
		this.dataSource = dataSource;
	}

	/**
	 * Verifies parameters, sets the parameters on {@link SimpleJdbcCallOperations}
	 * and ensures the appropriate {@link SqlParameterSourceFactory} is defined
	 * when {@link ProcedureParameter} are passed in.
	 */
	@Override
	public void afterPropertiesSet() {
		Assert.notNull(this.storedProcedureNameExpression,
				"You must either provide a Stored Procedure Name or a Stored Procedure Name Expression.");

		if (this.procedureParameters != null) {
			if (this.sqlParameterSourceFactory == null) {
				ExpressionEvaluatingSqlParameterSourceFactory expressionSourceFactory =
						new ExpressionEvaluatingSqlParameterSourceFactory();

				expressionSourceFactory.setBeanFactory(this.beanFactory);
				expressionSourceFactory
						.setStaticParameters(ProcedureParameter.convertStaticParameters(this.procedureParameters));
				expressionSourceFactory
						.setParameterExpressions(ProcedureParameter.convertExpressions(this.procedureParameters));

				this.sqlParameterSourceFactory = expressionSourceFactory;
			}
			else {
				Assert.isInstanceOf(ExpressionEvaluatingSqlParameterSourceFactory.class,
						this.sqlParameterSourceFactory,
						() -> "You are providing 'ProcedureParameters'. "
								+ "Was expecting the the provided 'sqlParameterSourceFactory' "
								+ "to be an instance of 'ExpressionEvaluatingSqlParameterSourceFactory', "
								+ "however the provided one is of type '"
								+ this.sqlParameterSourceFactory.getClass().getName() + "'");
			}

			if (this.usePayloadAsParameterSource == null) {
				this.usePayloadAsParameterSource = false;
			}
		}
		else {
			if (this.sqlParameterSourceFactory == null) {
				this.sqlParameterSourceFactory = new BeanPropertySqlParameterSourceFactory();
			}
			if (this.usePayloadAsParameterSource == null) {
				this.usePayloadAsParameterSource = true;
			}
		}

		this.jdbcCallOperationsMap = buildJdbcCallOperationsMap();

		this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(this.beanFactory);
	}

	private Map<String, SimpleJdbcCallOperations> buildJdbcCallOperationsMap() {
		return new LinkedHashMap<String, SimpleJdbcCallOperations>(this.jdbcCallOperationsCacheSize + 1, LOAD_FACTOR,
				true) {

			private static final long serialVersionUID = 3801124242820219131L;

			@Override
			protected boolean removeEldestEntry(Entry<String, SimpleJdbcCallOperations> eldest) {
				return size() > StoredProcExecutor.this.jdbcCallOperationsCacheSize;
			}

		};
	}

	private SimpleJdbcCall createSimpleJdbcCall(String storedProcedureName) {
		SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(this.dataSource);
		if (this.isFunction) {
			simpleJdbcCall.withFunctionName(storedProcedureName);
		}
		else {
			simpleJdbcCall.withProcedureName(storedProcedureName);
		}

		if (this.ignoreColumnMetaData) {
			simpleJdbcCall.withoutProcedureColumnMetaDataAccess();
		}

		simpleJdbcCall.declareParameters(this.sqlParameters.toArray(new SqlParameter[0]));

		if (!this.returningResultSetRowMappers.isEmpty()) {
			for (Entry<String, RowMapper<?>> mapEntry : this.returningResultSetRowMappers.entrySet()) {
				simpleJdbcCall.returningResultSet(mapEntry.getKey(), mapEntry.getValue());
			}
		}

		if (this.returnValueRequired) {
			simpleJdbcCall.withReturnValue();
		}

		simpleJdbcCall.getJdbcTemplate().setSkipUndeclaredResults(this.skipUndeclaredResults);

		return simpleJdbcCall;
	}

	/**
	 * Execute a Stored Procedure or Function - Use when no {@link Message} is
	 * available to extract {@link ProcedureParameter} values from it.
	 * @return Map containing the stored procedure results if any.
	 */
	public Map<String, Object> executeStoredProcedure() {
		return executeStoredProcedureInternal(new Object(), evaluateExpression(null));
	}

	/**
	 * Execute a Stored Procedure or Function - Use with {@link Message} is
	 * available to extract {@link ProcedureParameter} values from it.
	 * @param message A message.
	 * @return Map containing the stored procedure results if any.
	 */
	public Map<String, Object> executeStoredProcedure(Message<?> message) {
		Assert.notNull(message, "The message parameter must not be null.");
		Assert.notNull(this.usePayloadAsParameterSource, "Property usePayloadAsParameterSource "
				+ "was Null. Did you call afterPropertiesSet()?");

		final Object input;

		if (this.usePayloadAsParameterSource) {
			input = message.getPayload();
		}
		else {
			input = message;
		}

		return executeStoredProcedureInternal(input, evaluateExpression(message));

	}

	private String evaluateExpression(@Nullable Message<?> message) {
		final String storedProcedureNameToUse = message == null
				? this.storedProcedureNameExpression.getValue(this.evaluationContext, String.class)
				: this.storedProcedureNameExpression.getValue(this.evaluationContext, message, String.class);

		Assert.hasText(storedProcedureNameToUse,
				() -> "Unable to resolve Stored Procedure/Function name for the provided Expression '"
						+ this.storedProcedureNameExpression.getExpressionString() + "'.");
		return storedProcedureNameToUse;
	}

	/**
	 * Execute the Stored Procedure using the passed in {@link Message} as a source
	 * for parameters.
	 * @param input The message is used to extract parameters for the stored procedure.
	 * @return A map containing the return values from the Stored Procedure call if any.
	 */
	private Map<String, Object> executeStoredProcedureInternal(Object input, String storedProcedureName) {
		Assert.notNull(this.sqlParameterSourceFactory, "Property sqlParameterSourceFactory "
				+ "was Null. Did you call afterPropertiesSet()?");

		SimpleJdbcCallOperations localSimpleJdbcCall = obtainSimpleJdbcCall(storedProcedureName);

		SqlParameterSource storedProcedureParameterSource =
				this.sqlParameterSourceFactory.createParameterSource(input);

		return localSimpleJdbcCall.execute(storedProcedureParameterSource);

	}

	private SimpleJdbcCallOperations obtainSimpleJdbcCall(String storedProcedureName) {
		SimpleJdbcCallOperations operations = this.jdbcCallOperationsMap.get(storedProcedureName);
		if (operations == null) {
			synchronized (this.jdbcCallOperationsMapMonitor) {
				operations =
						this.jdbcCallOperationsMap.computeIfAbsent(storedProcedureName, this::createSimpleJdbcCall);
			}
		}
		return operations;
	}

	//~~~~~Setters for Properties~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * For fully supported databases, the underlying  {@link SimpleJdbcCall} can
	 * retrieve the parameter information for the to be invoked Stored Procedure
	 * from the JDBC Meta-data. However, if the used database does not support
	 * meta data lookups or if you like to provide customized parameter definitions,
	 * this flag can be set to 'true'. It defaults to 'false'.
	 * @param ignoreColumnMetaData true to ignore column metadata.
	 */
	public void setIgnoreColumnMetaData(boolean ignoreColumnMetaData) {
		this.ignoreColumnMetaData = ignoreColumnMetaData;
	}

	/**
	 * Custom Stored Procedure parameters that may contain static values
	 * or Strings representing an {@link Expression}.
	 * @param procedureParameters The parameters.
	 */
	public void setProcedureParameters(List<ProcedureParameter> procedureParameters) {
		Assert.notEmpty(procedureParameters, "'procedureParameters' must not be null or empty.");
		Assert.noNullElements(procedureParameters.toArray(), "'procedureParameters' cannot contain null values.");
		this.procedureParameters = procedureParameters;
	}

	/**
	 * If you database system is not fully supported by Spring and thus obtaining
	 * parameter definitions from the JDBC Meta-data is not possible, you must define
	 * the {@link SqlParameter} explicitly.
	 * @param sqlParameters The parameters.
	 */
	public void setSqlParameters(List<SqlParameter> sqlParameters) {
		Assert.notEmpty(sqlParameters, "'sqlParameters' must not be null or empty.");
		Assert.noNullElements(sqlParameters.toArray(), "'sqlParameters' cannot contain null values.");
		this.sqlParameters = sqlParameters;
	}

	/**
	 * Provides the ability to set a custom {@link SqlParameterSourceFactory}.
	 * Keep in mind that if {@link ProcedureParameter} are set explicitly and
	 * you would like to provide a custom {@link SqlParameterSourceFactory},
	 * then you must provide an instance of {@link ExpressionEvaluatingSqlParameterSourceFactory}.
	 * If not the SqlParameterSourceFactory will be replaced the default
	 * {@link ExpressionEvaluatingSqlParameterSourceFactory}.
	 * @param sqlParameterSourceFactory the parameter source factory.
	 */
	public void setSqlParameterSourceFactory(SqlParameterSourceFactory sqlParameterSourceFactory) {
		Assert.notNull(sqlParameterSourceFactory, "sqlParameterSourceFactory must not be null.");
		this.sqlParameterSourceFactory = sqlParameterSourceFactory;
	}

	/**
	 * @return the name of the Stored Procedure or Function if set. Null otherwise.
	 * */
	public String getStoredProcedureName() {
		return this.storedProcedureNameExpression instanceof LiteralExpression ?
				this.storedProcedureNameExpression.getValue(String.class) : null;
	}

	/**
	 * @return the Stored Procedure Name Expression as a String if set. Null otherwise.
	 * */
	public String getStoredProcedureNameExpressionAsString() {
		return this.storedProcedureNameExpression != null
				? this.storedProcedureNameExpression.getExpressionString()
				: null;
	}

	/**
	 * The name of the Stored Procedure or Stored Function to be executed.
	 * If {@link StoredProcExecutor#isFunction} is set to "true", then this
	 * property specifies the Stored Function name.
	 * Alternatively you can also specify the Stored Procedure name via
	 * {@link StoredProcExecutor#setStoredProcedureNameExpression(Expression)}.
	 * E.g., that way you can specify the name of the Stored Procedure or Stored Function
	 * through {@link org.springframework.messaging.MessageHeaders}.
	 * @param storedProcedureName Must not be null and must not be empty
	 * @see StoredProcExecutor#setStoredProcedureNameExpression(Expression)
	 */
	public void setStoredProcedureName(String storedProcedureName) {
		Assert.hasText(storedProcedureName, "storedProcedureName must not be null and cannot be empty.");
		this.storedProcedureNameExpression = new LiteralExpression(storedProcedureName);
	}

	/**
	 * Using the {@link StoredProcExecutor#storedProcedureNameExpression} the
	 * {@link Message} can be used as source for the name of the
	 * Stored Procedure or Stored Function.
	 * If {@link StoredProcExecutor#isFunction} is set to "true", then this
	 * property specifies the Stored Function name.
	 * By providing a SpEL expression as value for this setter, a subset of the
	 * original payload, a header value or any other resolvable SpEL expression
	 * can be used as the basis for the Stored Procedure / Function.
	 * For the Expression evaluation the full message is available as the <b>root object</b>.
	 * For instance the following SpEL expressions (among others) are possible:
	 * <ul>
	 * <li>payload.foo</li>
	 *    <li>headers.foobar</li>
	 *    <li>new java.util.Date()</li>
	 *    <li>'foo' + 'bar'</li>
	 * </ul>
	 * Alternatively you can also specify the Stored Procedure name via
	 * {@link StoredProcExecutor#setStoredProcedureName(String)}
	 * @param storedProcedureNameExpression Must not be null.
	 *
	 */
	public void setStoredProcedureNameExpression(Expression storedProcedureNameExpression) {
		Assert.notNull(storedProcedureNameExpression, "storedProcedureNameExpression must not be null.");
		this.storedProcedureNameExpression = storedProcedureNameExpression;
	}

	/**
	 * If set to 'true', the payload of the Message will be used as a source for
	 * providing parameters. If false the entire {@link Message} will be available
	 * as a source for parameters.
	 * If no {@link ProcedureParameter} are passed in, this property will default to
	 * <code>true</code>. This means that using a default {@link BeanPropertySqlParameterSourceFactory}
	 * the bean properties of the payload will be used as a source for parameter
	 * values for the to-be-executed Stored Procedure or Function.
	 * However, if {@link ProcedureParameter}s are passed in, then this property
	 * will by default evaluate to <code>false</code>. {@link ProcedureParameter}
	 * allow for SpEl Expressions to be provided and therefore it is highly
	 * beneficial to have access to the entire {@link Message}.
	 * @param usePayloadAsParameterSource If false the entire {@link Message} is used as parameter source.
	 */
	public void setUsePayloadAsParameterSource(boolean usePayloadAsParameterSource) {
		this.usePayloadAsParameterSource = usePayloadAsParameterSource;
	}

	/**
	 * Indicates whether a Stored Procedure or a Function is being executed.
	 * The default value is false.
	 * @param isFunction If set to true an Sql Function is executed rather than a Stored Procedure.
	 */
	public void setIsFunction(boolean isFunction) {
		this.isFunction = isFunction;
	}

	/**
	 * Indicates the procedure's return value should be included in the results
	 * returned.
	 * @param returnValueRequired true to include the return value.
	 */
	public void setReturnValueRequired(boolean returnValueRequired) {
		this.returnValueRequired = returnValueRequired;
	}

	/**
	 * If this variable is set to <code>true</code> then all results from a stored
	 * procedure call that don't have a corresponding
	 * {@link org.springframework.jdbc.core.SqlOutParameter}
	 * declaration will be bypassed.
	 * E.g. Stored Procedures may return an update count value, even though your
	 * Stored Procedure only declared a single result parameter. The exact behavior
	 * depends on the used database.
	 * The value is set on the underlying {@link org.springframework.jdbc.core.JdbcTemplate}.
	 * Only few developers will probably ever like to process update counts, thus
	 * the value defaults to <code>true</code>.
	 * @param skipUndeclaredResults The boolean.
	 */
	public void setSkipUndeclaredResults(boolean skipUndeclaredResults) {
		this.skipUndeclaredResults = skipUndeclaredResults;
	}

	/**
	 * If the Stored Procedure returns ResultSets you may provide a map of
	 * {@link RowMapper} to convert the {@link java.sql.ResultSet} to meaningful objects.
	 * @param returningResultSetRowMappers The map may not be null and must not contain null values.
	 */
	public void setReturningResultSetRowMappers(Map<String, RowMapper<?>> returningResultSetRowMappers) {
		Assert.notNull(returningResultSetRowMappers, "'returningResultSetRowMappers' must not be null.");
		Assert.noNullElements(returningResultSetRowMappers.values().toArray(),
				"'returningResultSetRowMappers' cannot contain null values.");
		this.returningResultSetRowMappers = returningResultSetRowMappers;
	}

	/**
	 * Defines the maximum number of {@link SimpleJdbcCallOperations}
	 * A value of zero will disable the cache. The default is 10.
	 * @param jdbcCallOperationsCacheSize Must not be negative.
	 */
	public void setJdbcCallOperationsCacheSize(int jdbcCallOperationsCacheSize) {
		Assert.isTrue(jdbcCallOperationsCacheSize >= 0, "jdbcCallOperationsCacheSize must not be negative.");
		this.jdbcCallOperationsCacheSize = jdbcCallOperationsCacheSize;
	}

	/**
	 * Allows to set the optional {@link BeanFactory} which is used to add a
	 * {@link org.springframework.expression.BeanResolver} to the
	 * {@link org.springframework.expression.spel.support.StandardEvaluationContext}.
	 * If not set this property defaults to null.
	 * @param beanFactory If set must not be null.
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

}


