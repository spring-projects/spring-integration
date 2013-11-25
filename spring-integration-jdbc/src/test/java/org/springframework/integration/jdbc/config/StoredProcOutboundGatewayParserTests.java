/*
 * Copyright 2002-2013 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.After;
import org.junit.Test;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.expression.Expression;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.jdbc.storedproc.PrimeMapper;
import org.springframework.integration.jdbc.storedproc.ProcedureParameter;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlInOutParameter;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.integration.core.MessagingTemplate;

/**
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author Artem Bilan
 * @since 2.1
 *
 */
public class StoredProcOutboundGatewayParserTests {

	private ConfigurableApplicationContext context;

	private EventDrivenConsumer outboundGateway;

	private static volatile int adviceCalled;

	@Test
	public void testProcedureNameIsSet() throws Exception {
		setUp("storedProcOutboundGatewayParserTest.xml", getClass());

		DirectFieldAccessor accessor = new DirectFieldAccessor(this.outboundGateway);
		Object source = accessor.getPropertyValue("handler");
		accessor = new DirectFieldAccessor(source);
		assertEquals(Boolean.TRUE, accessor.getPropertyValue("requiresReply"));
		source = accessor.getPropertyValue("executor");
		accessor = new DirectFieldAccessor(source);
		Expression  storedProcedureName = (Expression) accessor.getPropertyValue("storedProcedureNameExpression");
		assertEquals("Wrong stored procedure name", "GET_PRIME_NUMBERS",  storedProcedureName.getValue());
	}

	@Test
	public void testReplyTimeoutIsSet() throws Exception {
		setUp("storedProcOutboundGatewayParserTest.xml", getClass());

		DirectFieldAccessor accessor = new DirectFieldAccessor(this.outboundGateway);
		Object source = accessor.getPropertyValue("handler");
		accessor = new DirectFieldAccessor(source);
		source = accessor.getPropertyValue("messagingTemplate");

		MessagingTemplate messagingTemplate = (MessagingTemplate) source;

		accessor = new DirectFieldAccessor(messagingTemplate);

		Long  sendTimeout = (Long) accessor.getPropertyValue("sendTimeout");
		assertEquals("Wrong sendTimeout", Long.valueOf(555L),  sendTimeout);

	}

	@Test
	public void testSkipUndeclaredResultsAttributeSet() throws Exception {
		setUp("storedProcOutboundGatewayParserTest.xml", getClass());

		DirectFieldAccessor accessor = new DirectFieldAccessor(this.outboundGateway);
		Object source = accessor.getPropertyValue("handler");
		accessor = new DirectFieldAccessor(source);
		source = accessor.getPropertyValue("executor");
		accessor = new DirectFieldAccessor(source);
		boolean  skipUndeclaredResults = (Boolean) accessor.getPropertyValue("skipUndeclaredResults");
		assertFalse(skipUndeclaredResults);
	}

	@Test
	public void testReturnValueRequiredAttributeSet() throws Exception {
		setUp("storedProcOutboundGatewayParserTest.xml", getClass());

		DirectFieldAccessor accessor = new DirectFieldAccessor(this.outboundGateway);
		Object source = accessor.getPropertyValue("handler");
		accessor = new DirectFieldAccessor(source);
		source = accessor.getPropertyValue("executor");
		accessor = new DirectFieldAccessor(source);
		boolean returnValueRequired = (Boolean) accessor.getPropertyValue("returnValueRequired");
		assertTrue(returnValueRequired);
	}

	@Test
	public void testIsFunctionAttributeSet() throws Exception {
		setUp("storedProcOutboundGatewayParserFunctionTest.xml", getClass());
		DirectFieldAccessor accessor = new DirectFieldAccessor(this.outboundGateway);
		Object source = accessor.getPropertyValue("handler");
		accessor = new DirectFieldAccessor(source);
		source = accessor.getPropertyValue("executor");
		accessor = new DirectFieldAccessor(source);
		boolean isFunction = (Boolean) accessor.getPropertyValue("isFunction");
		assertTrue(isFunction);
	}

	@Test
	public void testIsFunctionAttributeSetToFalse() throws Exception {
		setUp("storedProcOutboundGatewayParserTest.xml", getClass());
		DirectFieldAccessor accessor = new DirectFieldAccessor(this.outboundGateway);
		Object source = accessor.getPropertyValue("handler");
		accessor = new DirectFieldAccessor(source);
		source = accessor.getPropertyValue("executor");
		accessor = new DirectFieldAccessor(source);
		boolean isFunction = (Boolean) accessor.getPropertyValue("isFunction");
		assertFalse(isFunction);
	}

	@Test
	public void testIsIgnoreColumnMetaDataSetToFalse() throws Exception {
		setUp("storedProcOutboundGatewayParserTest.xml", getClass());
		DirectFieldAccessor accessor = new DirectFieldAccessor(this.outboundGateway);
		Object source = accessor.getPropertyValue("handler");
		accessor = new DirectFieldAccessor(source);
		source = accessor.getPropertyValue("executor");
		accessor = new DirectFieldAccessor(source);
		boolean ignoreColumnMetaData = (Boolean) accessor.getPropertyValue("ignoreColumnMetaData");
		assertFalse(ignoreColumnMetaData);
	}

	@Test
	public void testIsIgnoreColumnMetaDataSet() throws Exception {
		setUp("storedProcOutboundGatewayParserFunctionTest.xml", getClass());
		DirectFieldAccessor accessor = new DirectFieldAccessor(this.outboundGateway);
		Object source = accessor.getPropertyValue("handler");
		accessor = new DirectFieldAccessor(source);
		source = accessor.getPropertyValue("executor");
		accessor = new DirectFieldAccessor(source);
		boolean ignoreColumnMetaData = (Boolean) accessor.getPropertyValue("ignoreColumnMetaData");
		assertTrue(ignoreColumnMetaData);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testProcedurepParametersAreSet() throws Exception {
		setUp("storedProcOutboundGatewayParserTest.xml", getClass());

		DirectFieldAccessor accessor = new DirectFieldAccessor(this.outboundGateway);
		Object source = accessor.getPropertyValue("handler");
		accessor = new DirectFieldAccessor(source);
		source = accessor.getPropertyValue("executor");
		accessor = new DirectFieldAccessor(source);
		Object  procedureParameters = accessor.getPropertyValue("procedureParameters");
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
		assertEquals(Integer.valueOf(30),  parameter4.getValue());

		assertNull(parameter1.getExpression());
		assertNull(parameter2.getExpression());
		assertEquals("payload.username", parameter3.getExpression());
		assertNull(parameter4.getExpression());

	}

	@SuppressWarnings("unchecked")
	@Test
	public void testReturningResultSetRowMappersAreSet() throws Exception {
		setUp("storedProcOutboundGatewayParserTest.xml", getClass());

		DirectFieldAccessor accessor = new DirectFieldAccessor(this.outboundGateway);
		Object source = accessor.getPropertyValue("handler");
		accessor = new DirectFieldAccessor(source);
		source = accessor.getPropertyValue("executor");
		accessor = new DirectFieldAccessor(source);
		Object  returningResultSetRowMappers = accessor.getPropertyValue("returningResultSetRowMappers");
		assertNotNull(returningResultSetRowMappers);
		assertTrue(returningResultSetRowMappers instanceof Map);

		Map<String, RowMapper<?>> returningResultSetRowMappersAsMap = (Map<String, RowMapper<?>>) returningResultSetRowMappers;

		assertTrue("The rowmapper was not set. Expected returningResultSetRowMappersAsMap.size() == 1", returningResultSetRowMappersAsMap.size() == 1);

		Entry<String, ?> mapEntry1 = returningResultSetRowMappersAsMap.entrySet().iterator().next();

		assertEquals("out",    mapEntry1.getKey());
		assertTrue(mapEntry1.getValue() instanceof PrimeMapper);

	}


	@SuppressWarnings("unchecked")
	@Test
	public void testSqlParametersAreSet() throws Exception {
		setUp("storedProcOutboundGatewayParserTest.xml", getClass());

		DirectFieldAccessor accessor = new DirectFieldAccessor(this.outboundGateway);
		Object source = accessor.getPropertyValue("handler");
		accessor = new DirectFieldAccessor(source);
		source = accessor.getPropertyValue("executor");
		accessor = new DirectFieldAccessor(source);
		Object  sqlParameters = accessor.getPropertyValue("sqlParameters");
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

	@Test
	public void advised() throws Exception {
		setUp("advisedStoredProcOutboundGatewayParserTest.xml", getClass());

		MessageHandler handler = TestUtils.getPropertyValue(this.outboundGateway, "handler", MessageHandler.class);
		handler.handleMessage(new GenericMessage<String>("foo"));
		assertEquals(1, adviceCalled);
	}

	@After
	public void tearDown(){
		if(context != null){
			context.close();
		}
	}

	public void setUp(String name, Class<?> cls){
		 this.context    = new ClassPathXmlApplicationContext(name, cls);
		 this.outboundGateway   = this.context.getBean("storedProcedureOutboundGateway", EventDrivenConsumer.class);
	}

	public static class FooAdvice extends AbstractRequestHandlerAdvice {

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) throws Exception {
			adviceCalled++;
			return null;
		}

	}
}
