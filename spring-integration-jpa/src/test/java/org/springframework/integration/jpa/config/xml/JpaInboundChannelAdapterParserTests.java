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

package org.springframework.integration.jpa.config.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.jpa.core.JpaExecutor;
import org.springframework.integration.jpa.core.JpaOperations;
import org.springframework.integration.test.util.TestUtils;

/**
 * @author Gunnar Hillert
 * @since 2.2
 *
 */
public class JpaInboundChannelAdapterParserTests {

    private ConfigurableApplicationContext context;

    private SourcePollingChannelAdapter consumer;

    @Test
    public void testJpaInboundChannelAdapterParser() throws Exception {
    	
        setUp("JpaInboundChannelAdapterParserTests.xml", getClass());

        final AbstractMessageChannel outputChannel = TestUtils.getPropertyValue(this.consumer, "outputChannel", AbstractMessageChannel.class); 
        
        assertEquals("out", outputChannel.getComponentName());
        
        final JpaExecutor jpaExecutor = TestUtils.getPropertyValue(this.consumer, "source.jpaExecutor", JpaExecutor.class);
        
        assertNotNull(jpaExecutor);
        
        final Class<?> entityClass = TestUtils.getPropertyValue(jpaExecutor, "entityClass", Class.class);
        
        assertEquals("org.springframework.integration.jpa.test.entity.Student", entityClass.getName());
        
        final JpaOperations jpaOperations = TestUtils.getPropertyValue(jpaExecutor, "jpaOperations", JpaOperations.class);
        
        assertNotNull(jpaOperations);
        
   }

    @After
    public void tearDown(){
        if(context != null){
            context.close();
        }
    }

    public void setUp(String name, Class<?> cls){
        context    = new ClassPathXmlApplicationContext(name, cls);
        consumer   = this.context.getBean("jpaInboundChannelAdapter", SourcePollingChannelAdapter.class);
    }

}
