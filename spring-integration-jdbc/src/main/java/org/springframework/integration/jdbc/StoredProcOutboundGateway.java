/*
 * Copyright 2002-2011 the original author or authors.
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

import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.Message;
import org.springframework.integration.MessagingException;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.jdbc.storedproc.ProcedureParameter;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.simple.SimpleJdbcCallOperations;
import org.springframework.jdbc.core.simple.SimpleJdbcTemplate;
import org.springframework.util.Assert;

/**
 * @author Gunnar Hillert
 *
 * @since 2.1
 */
public class StoredProcOutboundGateway extends AbstractReplyProducingMessageHandler implements InitializingBean {

	private final StoredProcExecutor executor;

	private volatile boolean expectSingleResult = false;
	
    /**
     * Constructor taking {@link DataSource} from which the DB Connection can be 
     * obtained and the name of the stored procedure or function to
     * execute to retrieve new rows.
     *
     * @param dataSource used to create a {@link SimpleJdbcTemplate}
     * @param storedProcedureName 
     */
    public StoredProcOutboundGateway(DataSource dataSource, String storedProcedureName) {
    	
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
    protected void onInit() {
    	super.onInit();
    	this.executor.afterPropertiesSet();
    };

    @Override
    protected Object handleRequestMessage(Message<?> requestMessage) {
    	
        Map<String, Object> resultMap = executor.executeStoredProcedure(requestMessage);
        
        final Object payload;
        
        if (resultMap.isEmpty()) {
            payload = null;
        } else {
        	
        	if (this.expectSingleResult && resultMap.size() == 1) {
                payload = resultMap.values().iterator().next();
        	} else if (this.expectSingleResult && resultMap.size() > 1) {
        		
        		throw new MessagingException(
        				"Stored Procedure/Function call returned more than "
        		      + "1 result object and expectSingleResult was 'true'. ");

        	} else {
        		payload = resultMap;
        	}
        
        }
        
        return MessageBuilder.withPayload(payload).copyHeaders(requestMessage.getHeaders()).build();
    
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
     * See also: {@link http://static.springsource.org/spring/docs/3.1.0.M2/spring-framework-reference/html/jdbc.html}
     */
    public void setSqlParameters(List<SqlParameter> sqlParameters) {
    	this.executor.setSqlParameters(sqlParameters);
    }

    /**
     *  Does your stored procedure return one or more result sets? If so, you
     *  can use the provided method for setting the respective Rowmappers.
     */
    public void setReturningResultSetRowMappers(
            Map<String, RowMapper<?>> returningResultSetRowMappers) {
    	this.executor.setReturningResultSetRowMappers(returningResultSetRowMappers);
    }

    /**
     *
     * @param ignoreColumnMetaData
     */
    public void setIgnoreColumnMetaData(boolean ignoreColumnMetaData) {
        this.executor.setIgnoreColumnMetaData(ignoreColumnMetaData);
    }

    /**
     *
     * @param returnValueRequired
     */
    public void setReturnValueRequired(boolean returnValueRequired) {
    	this.executor.setReturnValueRequired(returnValueRequired);
    }
    
	public void setProcedureParameters(List<ProcedureParameter> procedureParameters) {
		this.executor.setProcedureParameters(procedureParameters);
	}
	
	public void setIsFunction(boolean isFunction) {
		this.executor.setFunction(isFunction);
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
	
}
