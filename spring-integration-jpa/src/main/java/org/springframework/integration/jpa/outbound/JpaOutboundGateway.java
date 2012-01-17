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

import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.Message;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.jpa.core.JpaExecutor;
import org.springframework.integration.jpa.inbound.JpaPollingChannelAdapter;
import org.springframework.integration.jpa.support.OutboundGatewayType;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.util.Assert;

/**
 * The Jpa Outbound Gateway will allow you to make outbound operations to either:
 * 
 * <ul>
 * 
 * <li>submit (insert, delete) data to a database using JPA</li> 
 * <li>retrieve (select) data from a database</li>
 * 
 * Depending on the selected operation mode, the outbound gateway will use either
 * a {@link JpaMessageHandler} for sending or a {@link JpaPollingChannelAdapter}
 * for retrieving data.
 * 
 * @author Gunnar Hillert
 *
 * @since 2.2
 * 
 */
public class JpaOutboundGateway extends AbstractReplyProducingMessageHandler implements InitializingBean {
	
	private final JpaExecutor   jpaExecutor;	
	private OutboundGatewayType gatewayType = OutboundGatewayType.UPDATING;
	
    /**
     * Constructor taking an {@link EntityManager} from which the DB Connection can be
     * obtained.
     *
     * @param entityManager Must not be null
     * 
     */
    public JpaOutboundGateway(JpaExecutor jpaExecutor) {
    	Assert.notNull(jpaExecutor, "jpaExecutor must not be null.");
    	this.jpaExecutor = jpaExecutor;
    }

    /**
     * 
     */
    @Override
    protected void onInit() {
    	super.onInit();
    }

    @Override
    protected Object handleRequestMessage(Message<?> requestMessage) {

    	final Object result;
    	
        if (OutboundGatewayType.RETRIEVING.equals(this.gatewayType)) {
        	
        	result = this.jpaExecutor.poll(requestMessage);
        	
        } else if (OutboundGatewayType.UPDATING.equals(this.gatewayType)) {
        	
        	result = this.jpaExecutor.executeOutboundJpaOperation(requestMessage);

        } else {
        	
        	throw new IllegalArgumentException(String.format("GatewayType  '%s' is not supported.", this.gatewayType));
        	
        }
        
        if (result == null) {
        	return null;
        } 
        
        return MessageBuilder.withPayload(result).copyHeaders(requestMessage.getHeaders()).build();	

    }

	public void setGatewayType(OutboundGatewayType gatewayType) {
		this.gatewayType = gatewayType;
	}

}
