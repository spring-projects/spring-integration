/*
 * Copyright 2002-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.jdbc.config;

import java.sql.Types;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.expression.Expression;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.jdbc.storedproc.PrimeMapper;
import org.springframework.integration.jdbc.storedproc.ProcedureParameter;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.SqlInOutParameter;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author Artem Bilan
 * @author Glenn Renfro
 *
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
		assertThat(accessor.getPropertyValue("requiresReply")).isEqualTo(Boolean.TRUE);
		source = accessor.getPropertyValue("executor");
		accessor = new DirectFieldAccessor(source);
		Expression storedProcedureName = (Expression) accessor.getPropertyValue("storedProcedureNameExpression");
		assertThat(storedProcedureName.getValue()).as("Wrong stored procedure name").isEqualTo("GET_PRIME_NUMBERS");
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

		Long sendTimeout = (Long) accessor.getPropertyValue("sendTimeout");
		assertThat(sendTimeout).as("Wrong sendTimeout").isEqualTo(Long.valueOf(555L));

	}

	@Test
	public void testSkipUndeclaredResultsAttributeSet() throws Exception {
		setUp("storedProcOutboundGatewayParserTest.xml", getClass());

		DirectFieldAccessor accessor = new DirectFieldAccessor(this.outboundGateway);
		Object source = accessor.getPropertyValue("handler");
		accessor = new DirectFieldAccessor(source);
		source = accessor.getPropertyValue("executor");
		accessor = new DirectFieldAccessor(source);
		boolean skipUndeclaredResults = (Boolean) accessor.getPropertyValue("skipUndeclaredResults");
		assertThat(skipUndeclaredResults).isFalse();
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
		assertThat(returnValueRequired).isTrue();
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
		assertThat(isFunction).isTrue();
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
		assertThat(isFunction).isFalse();
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
		assertThat(ignoreColumnMetaData).isFalse();
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
		assertThat(ignoreColumnMetaData).isTrue();
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testProcedureParametersAreSet() throws Exception {
		setUp("storedProcOutboundGatewayParserTest.xml", getClass());

		DirectFieldAccessor accessor = new DirectFieldAccessor(this.outboundGateway);
		Object source = accessor.getPropertyValue("handler");
		accessor = new DirectFieldAccessor(source);
		source = accessor.getPropertyValue("executor");
		accessor = new DirectFieldAccessor(source);
		Object procedureParameters = accessor.getPropertyValue("procedureParameters");
		assertThat(procedureParameters).isNotNull();
		assertThat(procedureParameters instanceof List).isTrue();

		List<ProcedureParameter> procedureParametersAsList = (List<ProcedureParameter>) procedureParameters;

		assertThat(procedureParametersAsList.size() == 4).isTrue();

		ProcedureParameter parameter1 = procedureParametersAsList.get(0);
		ProcedureParameter parameter2 = procedureParametersAsList.get(1);
		ProcedureParameter parameter3 = procedureParametersAsList.get(2);
		ProcedureParameter parameter4 = procedureParametersAsList.get(3);

		assertThat(parameter1.getName()).isEqualTo("username");
		assertThat(parameter2.getName()).isEqualTo("description");
		assertThat(parameter3.getName()).isEqualTo("password");
		assertThat(parameter4.getName()).isEqualTo("age");

		assertThat(parameter1.getValue()).isEqualTo("kenny");
		assertThat(parameter2.getValue()).isEqualTo("Who killed Kenny?");
		assertThat(parameter3.getValue()).isNull();
		assertThat(parameter4.getValue()).isEqualTo(30);

		assertThat(parameter1.getExpression()).isNull();
		assertThat(parameter2.getExpression()).isNull();
		assertThat(parameter3.getExpression()).isEqualTo("payload.username");
		assertThat(parameter4.getExpression()).isNull();

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
		Object returningResultSetRowMappers = accessor.getPropertyValue("returningResultSetRowMappers");
		assertThat(returningResultSetRowMappers).isNotNull();
		assertThat(returningResultSetRowMappers instanceof Map).isTrue();

		Map<String, RowMapper<?>> returningResultSetRowMappersAsMap = (Map<String, RowMapper<?>>) returningResultSetRowMappers;

		assertThat(returningResultSetRowMappersAsMap.size() == 1)
				.as("The rowmapper was not set. Expected returningResultSetRowMappersAsMap.size() == 1").isTrue();

		Entry<String, ?> mapEntry1 = returningResultSetRowMappersAsMap.entrySet().iterator().next();

		assertThat(mapEntry1.getKey()).isEqualTo("out");
		assertThat(mapEntry1.getValue() instanceof PrimeMapper).isTrue();

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
		Object sqlParameters = accessor.getPropertyValue("sqlParameters");
		assertThat(sqlParameters).isNotNull();
		assertThat(sqlParameters instanceof List).isTrue();

		List<SqlParameter> sqlParametersAsList = (List<SqlParameter>) sqlParameters;

		assertThat(sqlParametersAsList.size() == 4).isTrue();

		SqlParameter parameter1 = sqlParametersAsList.get(0);
		SqlParameter parameter2 = sqlParametersAsList.get(1);
		SqlParameter parameter3 = sqlParametersAsList.get(2);
		SqlParameter parameter4 = sqlParametersAsList.get(3);

		assertThat(parameter1.getName()).isEqualTo("username");
		assertThat(parameter2.getName()).isEqualTo("password");
		assertThat(parameter3.getName()).isEqualTo("age");
		assertThat(parameter4.getName()).isEqualTo("description");

		assertThat(parameter1.getScale()).as("Expect that the scale is null.").isNull();
		assertThat(parameter2.getScale()).as("Expect that the scale is null.").isNull();
		assertThat(parameter3.getScale()).as("Expect that the scale is 5.").isEqualTo(Integer.valueOf(5));
		assertThat(parameter4.getScale()).as("Expect that the scale is null.").isNull();

		assertThat(parameter1.getSqlType()).as("SqlType is ").isEqualTo(Types.VARCHAR);
		assertThat(parameter2.getSqlType()).as("SqlType is ").isEqualTo(Types.VARCHAR);
		assertThat(parameter3.getSqlType()).as("SqlType is ").isEqualTo(Types.INTEGER);
		assertThat(parameter4.getSqlType()).as("SqlType is ").isEqualTo(Types.VARCHAR);

		assertThat(parameter2 instanceof SqlOutParameter).isTrue();
		assertThat(parameter3 instanceof SqlInOutParameter).isTrue();
	}

	@Test
	public void advised() throws Exception {
		setUp("advisedStoredProcOutboundGatewayParserTest.xml", getClass());

		MessageHandler handler = TestUtils.getPropertyValue(this.outboundGateway, "handler");
		handler.handleMessage(new GenericMessage<String>("foo"));
		assertThat(adviceCalled).isEqualTo(1);
	}

	@AfterEach
	public void tearDown() {
		if (context != null) {
			context.close();
		}
	}

	public void setUp(String name, Class<?> cls) {
		this.context = new ClassPathXmlApplicationContext(name, cls);
		this.outboundGateway = this.context.getBean("storedProcedureOutboundGateway", EventDrivenConsumer.class);
	}

	public static class FooAdvice extends AbstractRequestHandlerAdvice {

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) {
			adviceCalled++;
			return null;
		}

	}

}
