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
package org.springframework.integration.jpa.inbound;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;

import org.springframework.integration.Message;
import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.jpa.core.JpaExecutor;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.util.Assert;

/**
 * Polling message source that produces messages from the result of the provided:
 * 
 * <ul>
 *     <li>entityClass</li>
 *     <li>JpQl Select Query</li>
 *     <li>Sql Native Query</li>
 *     <li>JpQl Named Query</li>
 *     <li>Sql Native Named Query</li>
 * </ul>
 * 
 * After the objects have been polled, it also possibly to either:
 * 
 * executes an update after the select possibly to updated the state of selected records
 * 
 * <ul>
 *     <li>executes an update (per retrieved object or for the entire payload)</li>
 *     <li>delete the retrieved object</li>
 * </ul>   
 * 
 * @author Amol Nayak
 * @author Gunnar Hillert
 * 
 * @since 2.2
 * 
 */
public class JpaPollingChannelAdapter extends IntegrationObjectSupport implements MessageSource<Object>{

	private final JpaExecutor  jpaExecutor;

    /**
     * Constructor taking an {@link EntityManagerFactory} from which the 
     * {@link EntityManager} can be obtained.
     *
     * @param entityManagerFactory Must not be null.
     */
    public JpaPollingChannelAdapter(JpaExecutor jpaExecutor) {
    	Assert.notNull(jpaExecutor, "jpaExecutor must not be null.");
    	this.jpaExecutor = jpaExecutor;
    }
    
	/**
	 * Check for mandatory attributes
	 */
    @Override
	protected void onInit() throws Exception {
         super.onInit();
	}

	//~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
    
	/**
	 * Executes the query. If a query result set contains one or more rows, the
	 * Message payload will contain either a List of Maps for each row or, if a
	 * RowMapper has been provided, the values mapped from those rows. If the
	 * query returns no rows, this method will return <code>null</code>.
	 */
	public Message<Object> receive() {

		Object payload = jpaExecutor.poll();
		if (payload == null) {
			return null;
		}
		return MessageBuilder.withPayload(payload).build();
	}
	
	@Override
	public String getComponentType(){
		return "jpa:inbound-channel-adapter";
	}

}
