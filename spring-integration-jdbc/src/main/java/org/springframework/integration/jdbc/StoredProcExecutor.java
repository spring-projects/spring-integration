/*
 * Copyright 2002-2014 the original author or authors.
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
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.sql.DataSource;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.expression.BeanResolver;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.jdbc.storedproc.ProcedureParameter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlInOutParameter;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.jdbc.core.simple.SimpleJdbcCallOperations;
import org.springframework.jmx.export.annotation.ManagedAttribute;
import org.springframework.jmx.export.annotation.ManagedMetric;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.Assert;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.CacheStats;
import com.google.common.cache.LoadingCache;



/**
 * This class is used by all Stored Procedure (Stored Function) components and
 * provides the core functionality to execute those.
 *
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Gary Russell
 * @since 2.1
 *
 */
@ManagedResource
public class StoredProcExecutor implements BeanFactoryAware, InitializingBean {

	private volatile EvaluationContext evaluationContext;

	private volatile BeanFactory beanFactory = null;

	private volatile int jdbcCallOperationsCacheSize = 10;

	/**
	 * Uses the {@link SimpleJdbcCall} implementation for executing Stored Procedures.
	 */
	private volatile LoadingCache<String, SimpleJdbcCallOperations> jdbcCallOperationsCache = null;

	private volatile Expression storedProcedureNameExpression;

	/**
	 * For fully supported databases, the underlying  {@link SimpleJdbcCall} can
	 * retrieve the parameter information for the to be invoked Stored Procedure
	 * or Function from the JDBC Meta-data. However, if the used database does
	 * not support meta data lookups or if you like to provide customized
	 * parameter definitions, this flag can be set to 'true'. It defaults to 'false'.
	 */
	private volatile boolean   ignoreColumnMetaData = false;

	/**
	 * If this variable is set to true then all results from a stored procedure call
	 * that don't have a corresponding SqlOutParameter declaration will be bypassed.
	 *
	 * The value is set on the underlying {@link JdbcTemplate}.
	 *
	 * Value defaults to <code>true</code>.
	 */
	private volatile boolean  skipUndeclaredResults = true;

	/**
	 * If your database system is not fully supported by Spring and thus obtaining
	 * parameter definitions from the JDBC Meta-data is not possible, you must define
	 * the {@link SqlParameter} explicitly. See also {@link SqlOutParameter} and
	 * {@link SqlInOutParameter}.
	 */
	private volatile List<SqlParameter> sqlParameters = new ArrayList<SqlParameter>(0);

	/**
	 * By default bean properties of the passed in {@link Message} will be used
	 * as a source for the Stored Procedure's input parameters. By default a
	 * {@link BeanPropertySqlParameterSourceFactory} will be used.
	 *
	 * This may be sufficient for basic use cases. For more sophisticated options
	 * consider passing in one or more {@link ProcedureParameter}.
	 */
	private volatile SqlParameterSourceFactory sqlParameterSourceFactory = null;

	/**
	 * Indicates that whether only the payload of the passed-in {@link Message}
	 * shall be used as a source of parameters.
	 *
	 * @see #setUsePayloadAsParameterSource(boolean)
	 */
	private volatile Boolean usePayloadAsParameterSource = null;

	/**
	 * Custom Stored Procedure parameters that may contain static values
	 * or Strings representing an {@link Expression}.
	 */
	private volatile List<ProcedureParameter>procedureParameters;

	private volatile boolean isFunction = false;
	private volatile boolean returnValueRequired = false;
	private volatile Map<String, RowMapper<?>> returningResultSetRowMappers = new HashMap<String, RowMapper<?>>(0);

	private final DataSource dataSource;

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~


	/**
	 * Constructor taking {@link DataSource} from which the DB Connection can be
	 * obtained.
	 *
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
	 * {@inheritDoc}
	 */
	@Override
	public void afterPropertiesSet() {

		if (this.storedProcedureNameExpression == null) {
			throw new IllegalArgumentException("You must either provide a "
				+ "Stored Procedure Name or a Stored Procedure Name Expression.");
		}

		if (this.procedureParameters != null ) {

			if (this.sqlParameterSourceFactory == null) {
				ExpressionEvaluatingSqlParameterSourceFactory expressionSourceFactory =
											  new ExpressionEvaluatingSqlParameterSourceFactory();

				expressionSourceFactory.setBeanFactory(this.beanFactory);
				expressionSourceFactory.setStaticParameters(ProcedureParameter.convertStaticParameters(procedureParameters));
				expressionSourceFactory.setParameterExpressions(ProcedureParameter.convertExpressions(procedureParameters));

				this.sqlParameterSourceFactory = expressionSourceFactory;

			}
			else {

				if (!(this.sqlParameterSourceFactory instanceof ExpressionEvaluatingSqlParameterSourceFactory)) {
					throw new IllegalStateException("You are providing 'ProcedureParameters'. "
						+ "Was expecting the the provided sqlParameterSourceFactory "
						+ "to be an instance of 'ExpressionEvaluatingSqlParameterSourceFactory', "
						+ "however the provided one is of type '" + this.sqlParameterSourceFactory.getClass().getName() + "'");
				}

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

		jdbcCallOperationsCache = CacheBuilder.newBuilder()
				.maximumSize(jdbcCallOperationsCacheSize)
				.recordStats()
				.build(new CacheLoader<String, SimpleJdbcCallOperations>() {
					@Override
					public SimpleJdbcCall load(String storedProcedureName) {
						return createSimpleJdbcCall(storedProcedureName);
					}
				});

		if (this.storedProcedureNameExpression instanceof LiteralExpression) {
			String storedProcedureName = this.storedProcedureNameExpression.getValue(String.class);
			final SimpleJdbcCall simpleJdbcCall = createSimpleJdbcCall(storedProcedureName);
			this.jdbcCallOperationsCache.put(storedProcedureName, simpleJdbcCall);
		}

		this.evaluationContext = ExpressionUtils.createStandardEvaluationContext(beanFactory);

	}

	private SimpleJdbcCall createSimpleJdbcCall(String storedProcedureName) {

		final SimpleJdbcCall simpleJdbcCall = new SimpleJdbcCall(this.dataSource);

		if (this.isFunction) {
			simpleJdbcCall.withFunctionName(storedProcedureName);
		}
		else {
			simpleJdbcCall.withProcedureName(storedProcedureName);
		}

		if (this.ignoreColumnMetaData) {
			simpleJdbcCall.withoutProcedureColumnMetaDataAccess();
		}

		simpleJdbcCall.declareParameters(this.sqlParameters.toArray(new SqlParameter[this.sqlParameters.size()]));


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
	 *
	 * @return Map containing the stored procedure results if any.
	 */
	public Map<String, Object> executeStoredProcedure() {
		return executeStoredProcedureInternal(new Object(), this.evaluateExpression(null));
	}

	/**
	 * Execute a Stored Procedure or Function - Use with {@link Message} is
	 * available to extract {@link ProcedureParameter} values from it.
	 *
	 * @param message A message.
	 * @return Map containing the stored procedure results if any.
	 */
	public Map<String, Object> executeStoredProcedure(Message<?> message) {

		Assert.notNull(message, "The message parameter must not be null.");
		Assert.notNull(usePayloadAsParameterSource, "Property usePayloadAsParameterSource "
												  + "was Null. Did you call afterPropertiesSet()?");

		final Object input;

		if (usePayloadAsParameterSource) {
			input = message.getPayload();
		}
		else {
			input = message;
		}

		return executeStoredProcedureInternal(input, evaluateExpression(message));

	}

	private String evaluateExpression(Message<?> message) {
		final String storedProcedureNameToUse = this.storedProcedureNameExpression.getValue(this.evaluationContext,
				message, String.class);

		Assert.hasText(storedProcedureNameToUse, String.format(
				"Unable to resolve Stored Procedure/Function name for the provided Expression '%s'.",
				this.storedProcedureNameExpression.getExpressionString()));
		return storedProcedureNameToUse;
	}

	/**
	 * Execute the Stored Procedure using the passed in {@link Message} as a source
	 * for parameters.
	 *
	 * @param message The message is used to extract parameters for the stored procedure.
	 * @return A map containing the return values from the Stored Procedure call if any.
	 */
	private Map<String, Object> executeStoredProcedureInternal(Object input, String storedProcedureName) {

		Assert.notNull(sqlParameterSourceFactory, "Property sqlParameterSourceFactory "
												+ "was Null. Did you call afterPropertiesSet()?");

		SimpleJdbcCallOperations localSimpleJdbcCall = this.jdbcCallOperationsCache.getUnchecked(storedProcedureName);

		SqlParameterSource storedProcedureParameterSource =
			sqlParameterSourceFactory.createParameterSource(input);

		return localSimpleJdbcCall.execute(storedProcedureParameterSource);

	}

	//~~~~~Setters for Properties~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

	/**
	 * For fully supported databases, the underlying  {@link SimpleJdbcCall} can
	 * retrieve the parameter information for the to be invoked Stored Procedure
	 * from the JDBC Meta-data. However, if the used database does not support
	 * meta data lookups or if you like to provide customized parameter definitions,
	 * this flag can be set to 'true'. It defaults to 'false'.
	 *
	 * @param ignoreColumnMetaData true to ignore column metadata.
	 */
	public void setIgnoreColumnMetaData(boolean ignoreColumnMetaData) {
		this.ignoreColumnMetaData = ignoreColumnMetaData;
	}

	/**
	 * Custom Stored Procedure parameters that may contain static values
	 * or Strings representing an {@link Expression}.
	 *
	 * @param procedureParameters The parameters.
	 */
	public void setProcedureParameters(List<ProcedureParameter> procedureParameters) {

		Assert.notEmpty(procedureParameters, "procedureParameters must not be null or empty.");

		for (ProcedureParameter procedureParameter : procedureParameters) {
			Assert.notNull(procedureParameter, "The provided list (procedureParameters) cannot contain null values.");
		}

		this.procedureParameters = procedureParameters;

	}

	/**
	 * If you database system is not fully supported by Spring and thus obtaining
	 * parameter definitions from the JDBC Meta-data is not possible, you must define
	 * the {@link SqlParameter} explicitly.
	 *
	 * @param sqlParameters The parameters.
	 */
	public void setSqlParameters(List<SqlParameter> sqlParameters) {
		Assert.notEmpty(sqlParameters, "sqlParameters must not be null or empty.");

		for (SqlParameter sqlParameter : sqlParameters) {
			Assert.notNull(sqlParameter, "The provided list (sqlParameters) cannot contain null values.");
		}

		this.sqlParameters = sqlParameters;
	}

	/**
	 * Provides the ability to set a custom {@link SqlParameterSourceFactory}.
	 * Keep in mind that if {@link ProcedureParameter} are set explicitly and
	 * you would like to provide a custom {@link SqlParameterSourceFactory},
	 * then you must provide an instance of {@link ExpressionEvaluatingSqlParameterSourceFactory}.
	 *
	 * If not the SqlParameterSourceFactory will be replaced the default
	 * {@link ExpressionEvaluatingSqlParameterSourceFactory}.
	 *
	 * @param sqlParameterSourceFactory The paramtere source factory.
	 */
	public void setSqlParameterSourceFactory(SqlParameterSourceFactory sqlParameterSourceFactory) {
		Assert.notNull(sqlParameterSourceFactory, "sqlParameterSourceFactory must not be null.");
		this.sqlParameterSourceFactory = sqlParameterSourceFactory;
	}

	/**
	 * @return the name of the Stored Procedure or Function if set. Null otherwise.
	 * */
	@ManagedAttribute(defaultValue="Null if not Set.")
	public String getStoredProcedureName() {
		return this.storedProcedureNameExpression instanceof LiteralExpression ?
				storedProcedureNameExpression.getValue(String.class) : null;
	}

	/**
	 * @return the Stored Procedure Name Expression as a String if set. Null otherwise.
	 * */
	@ManagedAttribute(defaultValue="Null if not Set.")
	public String getStoredProcedureNameExpressionAsString() {
		return this.storedProcedureNameExpression != null ? this.storedProcedureNameExpression.getExpressionString() : null;
	}

	/**
	 * The name of the Stored Procedure or Stored Function to be executed.
	 * If {@link StoredProcExecutor#isFunction} is set to "true", then this
	 * property specifies the Stored Function name.
	 *
	 * Alternatively you can also specify the Stored Procedure name via
	 * {@link StoredProcExecutor#setStoredProcedureNameExpression(Expression)}.
	 *
	 * E.g., that way you can specify the name of the Stored Procedure or Stored Function
	 * through {@link MessageHeaders}.
	 *
	 * @param storedProcedureName Must not be null and must not be empty
	 *
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
	 *
	 * If {@link StoredProcExecutor#isFunction} is set to "true", then this
	 * property specifies the Stored Function name.
	 *
	 * By providing a SpEL expression as value for this setter, a subset of the
	 * original payload, a header value or any other resolvable SpEL expression
	 * can be used as the basis for the Stored Procedure / Function.
	 *
	 * For the Expression evaluation the full message is available as the <b>root object</b>.
	 *
	 * For instance the following SpEL expressions (among others) are possible:
	 *
	 * <ul>
	 * <li>payload.foo</li>
	 *    <li>headers.foobar</li>
	 *    <li>new java.util.Date()</li>
	 *    <li>'foo' + 'bar'</li>
	 * </ul>
	 *
	 * Alternatively you can also specify the Stored Procedure name via
	 * {@link StoredProcExecutor#setStoredProcedureName(String)}
	 *
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
	 *
	 * If no {@link ProcedureParameter} are passed in, this property will default to
	 * <code>true</code>. This means that using a default {@link BeanPropertySqlParameterSourceFactory}
	 * the bean properties of the payload will be used as a source for parameter
	 * values for the to-be-executed Stored Procedure or Function.
	 *
	 * However, if {@link ProcedureParameter}s are passed in, then this property
	 * will by default evaluate to <code>false</code>. {@link ProcedureParameter}
	 * allow for SpEl Expressions to be provided and therefore it is highly
	 * beneficial to have access to the entire {@link Message}.
	 *
	 * @param usePayloadAsParameterSource If false the entire {@link Message} is used as parameter source.
	 */
	public void setUsePayloadAsParameterSource(boolean usePayloadAsParameterSource) {
		this.usePayloadAsParameterSource = usePayloadAsParameterSource;
	}

	/**
	 * Indicates whether a Stored Procedure or a Function is being executed.
	 * The default value is false.
	 *
	 * @param isFunction If set to true an Sql Function is executed rather than a Stored Procedure.
	 */
	public void setIsFunction(boolean isFunction) {
		this.isFunction = isFunction;
	}

	/**
	 * Indicates the procedure's return value should be included in the results
	 * returned.
	 *
	 * @param returnValueRequired true to include the return value.
	 */
	public void setReturnValueRequired(boolean returnValueRequired) {
		this.returnValueRequired = returnValueRequired;
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
	 * @param skipUndeclaredResults The boolean.
	 *
	 */
	public void setSkipUndeclaredResults(boolean skipUndeclaredResults) {
		this.skipUndeclaredResults = skipUndeclaredResults;
	}

	/**
	 * If the Stored Procedure returns ResultSets you may provide a map of
	 * {@link RowMapper} to convert the {@link ResultSet} to meaningful objects.
	 *
	 * @param returningResultSetRowMappers The map may not be null and must not contain null values.
	 */
	public void setReturningResultSetRowMappers(Map<String, RowMapper<?>> returningResultSetRowMappers) {

		Assert.notNull(returningResultSetRowMappers, "returningResultSetRowMappers must not be null.");

		for (RowMapper<?> rowMapper : returningResultSetRowMappers.values()) {
			Assert.notNull(rowMapper, "The provided map cannot contain null values.");
		}

		this.returningResultSetRowMappers = returningResultSetRowMappers;
	}

	/**
	 * Allows for the retrieval of metrics ({@link CacheStats}}) for the
	 * {@link StoredProcExecutor#jdbcCallOperationsCache}, which is used to store
	 * instances of {@link SimpleJdbcCallOperations}.
	 *
	 * @return Cache statistics for {@link StoredProcExecutor#jdbcCallOperationsCache}
	 */
	public CacheStats getJdbcCallOperationsCacheStatistics() {
		return this.jdbcCallOperationsCache.stats();
	}

	/**
	 * Allows for the retrieval of metrics ({@link CacheStats}}) for the
	 * {@link StoredProcExecutor#jdbcCallOperationsCache}.
	 *
	 * Provides the properties of {@link CacheStats} as a {@link Map}. This allows
	 * for exposing the those properties easily via JMX.
	 *
	 * @return Map containing metrics of the JdbcCallOperationsCache
	 *
	 * @see StoredProcExecutor#getJdbcCallOperationsCacheStatistics()
	 */
	@ManagedMetric
	public Map<String, Object> getJdbcCallOperationsCacheStatisticsAsMap() {
		final CacheStats cacheStats = this.getJdbcCallOperationsCacheStatistics();
		final Map<String, Object> cacheStatistics  = new HashMap<String, Object>(11);
		cacheStatistics.put("averageLoadPenalty", cacheStats.averageLoadPenalty());
		cacheStatistics.put("evictionCount", cacheStats.evictionCount());
		cacheStatistics.put("hitCount", cacheStats.hitCount());
		cacheStatistics.put("hitRate", cacheStats.hitRate());
		cacheStatistics.put("loadCount", cacheStats.loadCount());
		cacheStatistics.put("loadExceptionCount", cacheStats.loadExceptionCount());
		cacheStatistics.put("loadExceptionRate", cacheStats.loadExceptionRate());
		cacheStatistics.put("loadSuccessCount", cacheStats.loadSuccessCount());
		cacheStatistics.put("missCount", cacheStats.missCount());
		cacheStatistics.put("missRate", cacheStats.missRate());
		cacheStatistics.put("totalLoadTime", cacheStats.totalLoadTime());
		return Collections.unmodifiableMap(cacheStatistics);
	}

	/**
	 * Defines the maximum number of {@link SimpleJdbcCallOperations}
	 * ({@link SimpleJdbcCall}) instances to be held by
	 * {@link StoredProcExecutor#jdbcCallOperationsCache}.
	 *
	 * A value of zero will disable the cache. The default is 10.
	 *
	 * @see CacheBuilder#maximumSize(long)
	 * @param jdbcCallOperationsCacheSize Must not be negative.
	 */
	public void setJdbcCallOperationsCacheSize(int jdbcCallOperationsCacheSize) {
		Assert.isTrue(jdbcCallOperationsCacheSize >= 0, "jdbcCallOperationsCacheSize must not be negative.");
		this.jdbcCallOperationsCacheSize = jdbcCallOperationsCacheSize;
	}

	/**
	 * Allows to set the optional {@link BeanFactory} which is used to add a
	 * {@link BeanResolver} to the {@link StandardEvaluationContext}. If not set
	 * this property defaults to null.
	 *
	 * @param beanFactory If set must not be null.
	 */
	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		Assert.notNull(returningResultSetRowMappers, "returningResultSetRowMappers must not be null.");
		this.beanFactory = beanFactory;
	}

}
