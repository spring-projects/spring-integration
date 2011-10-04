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

package org.springframework.integration.jdbc.config;

import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import java.sql.Types;
import java.util.List;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.jdbc.storedproc.ProcedureParameter;
import org.springframework.jdbc.core.SqlInOutParameter;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;

/**
 * @author Gunnar Hillert
 * @since 2.1
 *
 */
public class StoredProcMessageHandlerParserTests {

    private ConfigurableApplicationContext context;

    private EventDrivenConsumer consumer;

    @Test
    public void testProcedureNameIsSet() throws Exception {
        setUp("basicStoredProcOutboundChannelAdapterTest.xml", getClass());

        DirectFieldAccessor accessor = new DirectFieldAccessor(this.consumer);
        Object handler = accessor.getPropertyValue("handler");
        accessor = new DirectFieldAccessor(handler);

        Object executor = accessor.getPropertyValue("executor");
        DirectFieldAccessor executorAccessor = new DirectFieldAccessor(executor);

        Object  testProcedure1 = executorAccessor.getPropertyValue("storedProcedureName");
        assertEquals("Resolution Required should be 'testProcedure1' but was " + testProcedure1, "testProcedure1",  testProcedure1);
    }

    @Test
    public void testProcedurepParametersAreSet() throws Exception {
        setUp("basicStoredProcOutboundChannelAdapterTest.xml", getClass());

        DirectFieldAccessor accessor = new DirectFieldAccessor(this.consumer);
        Object handler = accessor.getPropertyValue("handler");
        accessor = new DirectFieldAccessor(handler);

        Object executor = accessor.getPropertyValue("executor");
        DirectFieldAccessor executorAccessor = new DirectFieldAccessor(executor);

        Object  procedureParameters = executorAccessor.getPropertyValue("procedureParameters");
        assertNotNull(procedureParameters);
        assertTrue(procedureParameters instanceof List);

        List<ProcedureParameter>procedureParametersAsList = (List<ProcedureParameter>) procedureParameters;

        assertTrue(procedureParametersAsList.size() == 4);

        ProcedureParameter parameter1 = procedureParametersAsList.get(0);
        ProcedureParameter parameter2 = procedureParametersAsList.get(1);
        ProcedureParameter parameter3 = procedureParametersAsList.get(2);
        ProcedureParameter parameter4 = procedureParametersAsList.get(3);

        assertEquals("username",    parameter1.getName());
        assertEquals("description", parameter2.getName());
        assertEquals("password",    parameter3.getName());
        assertEquals("age",         parameter4.getName());

        assertEquals("kenny",              parameter1.getValue());
        assertEquals("Who killed Kenny?",  parameter2.getValue());
        assertNull(parameter3.getValue());
        assertEquals(Integer.valueOf(30),  (Integer) parameter4.getValue());

        assertNull(parameter1.getExpression());
        assertNull(parameter2.getExpression());
        assertEquals("payload.username", parameter3.getExpression());
        assertNull(parameter4.getExpression());

    }

    @Test
    public void testSqlParametersAreSet() throws Exception {
        setUp("basicStoredProcOutboundChannelAdapterTest.xml", getClass());

        DirectFieldAccessor accessor = new DirectFieldAccessor(this.consumer);
        Object handler = accessor.getPropertyValue("handler");
        accessor = new DirectFieldAccessor(handler);

        Object executor = accessor.getPropertyValue("executor");
        DirectFieldAccessor executorAccessor = new DirectFieldAccessor(executor);

        Object  sqlParameters = executorAccessor.getPropertyValue("sqlParameters");

        assertNotNull(sqlParameters);
        assertTrue(sqlParameters instanceof List);

        List<SqlParameter>sqlParametersAsList = (List<SqlParameter>) sqlParameters;

        assertTrue(sqlParametersAsList.size() == 4);

        SqlParameter parameter1 = sqlParametersAsList.get(0);
        SqlParameter parameter2 = sqlParametersAsList.get(1);
        SqlParameter parameter3 = sqlParametersAsList.get(2);
        SqlParameter parameter4 = sqlParametersAsList.get(3);

        assertEquals("username",    parameter1.getName());
        assertEquals("password",    parameter2.getName());
        assertEquals("age",         parameter3.getName());
        assertEquals("description", parameter4.getName());

        assertNull("Expect that the scale is null.", parameter1.getScale());
        assertNull("Expect that the scale is null.", parameter2.getScale());
        assertEquals("Expect that the scale is 5.", Integer.valueOf(5),  parameter3.getScale());
        assertNull("Expect that the scale is null.", parameter4.getScale());

        assertEquals("SqlType is ", Types.VARCHAR, parameter1.getSqlType());
        assertEquals("SqlType is ", Types.VARCHAR, parameter2.getSqlType());
        assertEquals("SqlType is ", Types.INTEGER, parameter3.getSqlType());
        assertEquals("SqlType is ", Types.VARCHAR, parameter4.getSqlType());

        assertTrue(parameter1 instanceof SqlParameter);
        assertTrue(parameter2 instanceof SqlOutParameter);
        assertTrue(parameter3 instanceof SqlInOutParameter);
        assertTrue(parameter4 instanceof SqlParameter);

    }

    @After
    public void tearDown(){
        if(context != null){
            context.close();
        }
    }

    public void setUp(String name, Class<?> cls){
        context    = new ClassPathXmlApplicationContext(name, cls);
        consumer   = this.context.getBean("storedProcedureOutboundChannelAdapter", EventDrivenConsumer.class);
    }

}
