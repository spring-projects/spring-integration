/*
 * Copyright 2002-2011 the original author or authors.
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.sql.DataSource;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.expression.Expression;
import org.springframework.integration.Message;
import org.springframework.integration.jdbc.storedproc.ProcedureParameter;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlInOutParameter;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.jdbc.core.simple.SimpleJdbcCallOperations;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.util.Assert;

/**
 * A message handler that executes Stored Procedures for update purposes.
 *
 * Stored procedure parameter value are by default automatically extracted from
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
 * values, only, the Stored Procedure message handler does purposefully not support
 * SQL function calls. If you believe there are valid use-cases for that, please file a
 * feature request at http://jira.springsource.org.
 *
 *
 * @author Gunnar Hillert
 * @since 2.1
 *
 */
public class StoredProcExecutor implements InitializingBean {

    /**
     * Uses the {@link SimpleJdbcCall} implementation for executing Stored Procedures.
     */
    private final SimpleJdbcCallOperations jdbcCallOperations;

    /**
     * Name of the stored procedure or function to be executed.
     */
    private volatile String    storedProcedureName;

    /**
     * For fully supported databases, the underlying  {@link SimpleJdbcCall} can
     * retrieve the parameter information for the to be invoked Stored Procedure
     * or Function from the JDBC Meta-data. However, if the used database does
     * not support meta data lookups or if you like to provide customized
     * parameter definitions, this flag can be set to 'true'. It defaults to 'false'.
     */
    private volatile boolean   ignoreColumnMetaData = false;

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
     * Indicates that whether only the payload of the passed in {@link Message}
     * will be used as a source of parameters. The is 'true' by default because as a
     * default a {@link BeanPropertySqlParameterSourceFactory} implementation is
     * used for the sqlParameterSourceFactory property.
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

    //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     * Constructor taking {@link DataSource} from which the DB Connection can be
     * obtained and the select query to execute the stored procedure.
     *
     * @param dataSource used to create a {@link SimpleJdbcTemplate}, must not be Null
     * @param storedProcedureName Name of the Stored Procedure of function, must not be empty
     */
    public StoredProcExecutor(DataSource dataSource, String storedProcedureName) {

        Assert.notNull(dataSource, "dataSource must not be null.");
        Assert.hasText(storedProcedureName, "storedProcedureName must not be null and cannot be empty.");

        this.jdbcCallOperations = new SimpleJdbcCall(dataSource);
        this.storedProcedureName = storedProcedureName;
    }

    /**
     * Verifies parameters, sets the parameters on {@link SimpleJdbcCallOperations}
     * and ensures the appropriate {@link SqlParameterSourceFactory} is defined
     * when {@link ProcedureParameter} are passed in.
     */
    public void afterPropertiesSet() {

        if (this.procedureParameters != null ) {

            if (this.sqlParameterSourceFactory == null) {
                ExpressionEvaluatingSqlParameterSourceFactory expressionSourceFactory =
                                              new ExpressionEvaluatingSqlParameterSourceFactory();

                expressionSourceFactory.setStaticParameters(ProcedureParameter.convertStaticParameters(procedureParameters));
                expressionSourceFactory.setParameterExpressions(ProcedureParameter.convertExpressions(procedureParameters));

                this.sqlParameterSourceFactory = expressionSourceFactory;

            } else {

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

        } else {

            if (this.sqlParameterSourceFactory == null) {
                this.sqlParameterSourceFactory = new BeanPropertySqlParameterSourceFactory();
            }

            if (this.usePayloadAsParameterSource == null) {
                this.usePayloadAsParameterSource = true;
            }

        }

        if (this.ignoreColumnMetaData) {
            this.jdbcCallOperations.withoutProcedureColumnMetaDataAccess();
        }

        this.jdbcCallOperations.declareParameters(this.sqlParameters.toArray(new SqlParameter[this.sqlParameters.size()]));


        if (!this.returningResultSetRowMappers.isEmpty()) {

            for (Entry<String, RowMapper<?>> mapEntry : this.returningResultSetRowMappers.entrySet()) {
                jdbcCallOperations.returningResultSet(mapEntry.getKey(), mapEntry.getValue());
            }
        }

        if (this.returnValueRequired) {
            jdbcCallOperations.withReturnValue();
        }

        if (this.isFunction) {
            this.jdbcCallOperations.withFunctionName(this.storedProcedureName);
        } else {
            this.jdbcCallOperations.withProcedureName(this.storedProcedureName);
        }

    }

    /**
     * Execute a Stored Procedure of Function - Use when not {@link Message} is
     * available to extract {@link ProcedureParameter} values from it.
     *
     * @return Map containing the stored procedure results if any.
     */
    public Map<String, Object> executeStoredProcedure() {
        return executeStoredProcedureInternal(new Object());
    }

    /**
     * Execute a Stored Procedure of Function - Use with {@link Message} is
     * available to extract {@link ProcedureParameter} values from it.
     *
     * @return Map containing the stored procedure results if any.
     */
    public Map<String, Object> executeStoredProcedure(Message<?> message) {

        Assert.notNull(message, "The message parameter must not be null.");
        Assert.notNull(usePayloadAsParameterSource, "Property usePayloadAsParameterSource "
                                                  + "was Null. Did you call afterPropertiesSet()?");

        if (usePayloadAsParameterSource) {
            return executeStoredProcedureInternal(message.getPayload());
        } else {
            return executeStoredProcedureInternal(message);
        }

    }

    /**
     * Execute the Stored Procedure using the passed in {@link Message} as a source
     * for parameters.
     *
     * @param message The message is used to extract parameters for the stored procedure.
     * @return A map containing the return values from the Stored Procedure call if any.
     */
    private Map<String, Object> executeStoredProcedureInternal(Object input) {

        Assert.notNull(sqlParameterSourceFactory, "Property sqlParameterSourceFactory "
                                                + "was Null. Did you call afterPropertiesSet()?");

        SqlParameterSource storedProcedureParameterSource =
            sqlParameterSourceFactory.createParameterSource(input);

        return StoredProcExecutor.executeStoredProcedure(jdbcCallOperations,
                                                         storedProcedureParameterSource);
    }

    /**
     */
    private static Map<String, Object> executeStoredProcedure(SimpleJdbcCallOperations simpleJdbcCallOperations,
                                                             SqlParameterSource storedProcedureParameterSource) {

        Map<String, Object> resultMap = simpleJdbcCallOperations.execute(storedProcedureParameterSource);

        return resultMap;

    }

    //~~~~~Setters for Properties~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

    /**
     * For fully supported databases, the underlying  {@link SimpleJdbcCall} can
     * retrieve the parameter information for the to be invoked Stored Procedure
     * from the JDBC Meta-data. However, if the used database does not support
     * meta data lookups or if you like to provide customized parameter definitions,
     * this flag can be set to 'true'. It defaults to 'false'.
     */
    public void setIgnoreColumnMetaData(boolean ignoreColumnMetaData) {
        this.ignoreColumnMetaData = ignoreColumnMetaData;
    }

    /**
     * Custom Stored Procedure parameters that may contain static values
     * or Strings representing an {@link Expression}.
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
     * @param sqlParameterSourceFactory
     */
    public void setSqlParameterSourceFactory(SqlParameterSourceFactory sqlParameterSourceFactory) {
        Assert.notNull(sqlParameterSourceFactory, "sqlParameterSourceFactory must not be null.");
        this.sqlParameterSourceFactory = sqlParameterSourceFactory;
    }

    /**
     * @return the name of the Stored Procedure or Function
     * */
    public String getStoredProcedureName() {
        return this.storedProcedureName;
    }

    public void setUsePayloadAsParameterSource(boolean usePayloadAsParameterSource) {
        this.usePayloadAsParameterSource = usePayloadAsParameterSource;
    }

    public void setFunction(boolean isFunction) {
        this.isFunction = isFunction;
    }

    public void setReturnValueRequired(boolean returnValueRequired) {
        this.returnValueRequired = returnValueRequired;
    }

    /**
     * If the Stored Procedure returns ResultSets you may provide a map of
     * {@link RowMapper} to convert the {@link ResultSet} to meaningful objects.
     *
     * @param returningResultSetRowMappers The map may not be null and must not contain null values.
     */
    public void setReturningResultSetRowMappers(
            Map<String, RowMapper<?>> returningResultSetRowMappers) {

        Assert.notNull(returningResultSetRowMappers, "returningResultSetRowMappers must not be null.");

        for (RowMapper<?> rowMapper : returningResultSetRowMappers.values()) {
            Assert.notNull(rowMapper, "The provided map cannot contain null values.");
        }

        this.returningResultSetRowMappers = returningResultSetRowMappers;
    }

}
