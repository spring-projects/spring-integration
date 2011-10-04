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

import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.expression.Expression;
import org.springframework.integration.Message;
import org.springframework.integration.MessageDeliveryException;
import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.MessageRejectedException;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.jdbc.storedproc.ProcedureParameter;
import org.springframework.jdbc.core.SqlParameter;
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
public class StoredProcMessageHandler extends AbstractMessageHandler implements InitializingBean {

	final StoredProcExecutor executor;
    
    /**
     * Constructor taking {@link DataSource} from which the DB Connection can be 
     * obtained and the name of the stored procedure or function to
     * execute to retrieve new rows.
     *
     * @param dataSource used to create a {@link SimpleJdbcTemplate}. Must not be null.
     * @param storedProcedureName The name of the Stored Procedure or Function. Must not be null.
     */
    public StoredProcMessageHandler(DataSource dataSource, String storedProcedureName) {
    	
    	Assert.notNull(dataSource, "dataSource must not be null.");
    	Assert.hasText(storedProcedureName, "storedProcedureName must not be null and cannot be empty.");
    	
    	this.executor = new StoredProcExecutor(dataSource, storedProcedureName);
    	this.executor.setUsePayloadAsParameterSource(true);
    }

    /**
     * Verifies parameters, sets the parameters on {@link SimpleJdbcCallOperations}
     * and ensures the appropriate {@link SqlParameterSourceFactory} is defined 
     * when {@link ProcedureParameter} are passed in.
     */
    @Override
    protected void onInit() throws Exception {
    	super.onInit();
    	this.executor.afterPropertiesSet();
    };
    
    /**
     * Executes the Stored procedure, delegates to executeStoredProcedure(...).
     * Any return values from the Stored procedure are ignored. 
     * 
     * Return values are logged at debug level, though. 
     */
    @Override
    protected void handleMessageInternal(Message<?> message) throws MessageRejectedException, MessageHandlingException,
            MessageDeliveryException {

    	Map<String, Object> resultMap = executor.executeStoredProcedure(message);
    	
    	if (logger.isDebugEnabled()) {
    		
    		if (resultMap != null && !resultMap.isEmpty()) {
	    		logger.debug(String.format("The StoredProcMessageHandler ignores return " 
	    	        + "values, but the called Stored Procedure '%s' returned the "
	    			+ "following data: '%s'", executor.getStoredProcedureName(), resultMap));
    		}
    		
    	}

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
		this.executor.setIgnoreColumnMetaData(ignoreColumnMetaData);
	}
	
    /**
     * Custom Stored Procedure parameters that may contain static values
     * or Strings representing an {@link Expression}.
     */
	public void setProcedureParameters(List<ProcedureParameter> procedureParameters) {
		this.executor.setProcedureParameters(procedureParameters);
	}
    
    /**
     * If you database system is not fully supported by Spring and thus obtaining 
     * parameter definitions from the JDBC Meta-data is not possible, you must define
     * the {@link SqlParameter} explicitly. 
     */
	public void setSqlParameters(List<SqlParameter> sqlParameters) {
		this.executor.setSqlParameters(sqlParameters);
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
    	this.executor.setSqlParameterSourceFactory(sqlParameterSourceFactory);
    }

}
