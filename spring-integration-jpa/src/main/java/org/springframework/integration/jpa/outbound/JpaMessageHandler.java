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
package org.springframework.integration.jpa.outbound;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.springframework.integration.Message;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.jpa.core.JpaExecutor;
import org.springframework.integration.jpa.core.JpaOperations;
import org.springframework.util.Assert;

/**
 * The message handler for &lt;int-jpa:outbound-channel-adapter/&gt;.
 * 
 * In order to initialize the adapter, you must provide one of the following:
 * 
 * <ul>
 *     <li>{@link EntityManagerFactory}</li>
 *     <li>{@link EntityManager}</li>
 *     <li>{@link JpaOperations}</li>
 * </ul>
 * 
 * @author Amol Nayak
 * @author Gunnar Hillert
 * 
 * @since 2.2
 * 
 */
public class JpaMessageHandler extends AbstractMessageHandler {

    private final JpaExecutor jpaExecutor;
    
    public JpaMessageHandler(JpaExecutor jpaExecutor) {
		super();
		Assert.notNull(jpaExecutor, "jpaExecutor must not be Null");
		this.jpaExecutor = jpaExecutor;
	}

	/**
     * Used to verify and initialize parameters.
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
    protected void handleMessageInternal(final Message<?> message) {

		final Object result = executeJpaOperation(message);
		
		if (logger.isDebugEnabled()) {
			logger.debug(String.format("Result: %s", result));			
		}
		
    }

    /**
     * Executes the actual Jpa Operation. Call this method, if you need access to
     * process return values. This methods return a Map that contains either 
     * the number of affected entities or the affected entity itself.
     * 
     * Keep in mind that the number of entities effected by the operation may 
     * not necessarily correlate with the number of rows effected in the database.
     *  
     * @param message
     * @return Either the number of affected entities when using a JPAQL query. When using a merge/persist the updated/inserted itself is returned.
     */
    protected Object executeJpaOperation(final Message<?> message) {
    	Assert.notNull(message, "The provided message must not be Null.");
        return this.jpaExecutor.executeOutboundJpaOperation(message);     
    }

}
