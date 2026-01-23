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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.expression.Expression;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.jdbc.storedproc.ProcedureParameter;
import org.springframework.integration.test.util.TestUtils;
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
public class StoredProcMessageHandlerParserTests {

	private ConfigurableApplicationContext context;

	private EventDrivenConsumer consumer;

	private static volatile int adviceCalled;

	@Test
	public void testProcedureNameIsSet() {
		setUp("basicStoredProcOutboundChannelAdapterTest.xml", getClass());

		DirectFieldAccessor accessor = new DirectFieldAccessor(this.consumer);
		Object handler = accessor.getPropertyValue("handler");
		accessor = new DirectFieldAccessor(handler);

		Object executor = accessor.getPropertyValue("executor");
		DirectFieldAccessor executorAccessor = new DirectFieldAccessor(executor);

		Expression testProcedure1 = (Expression) executorAccessor.getPropertyValue("storedProcedureNameExpression");
		assertThat(testProcedure1.getValue())
				.as("Resolution Required should be 'testProcedure1' but was " + testProcedure1)
				.isEqualTo("testProcedure1");
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testProcedureParametersAreSet() {
		setUp("basicStoredProcOutboundChannelAdapterTest.xml", getClass());

		DirectFieldAccessor accessor = new DirectFieldAccessor(this.consumer);
		Object handler = accessor.getPropertyValue("handler");
		accessor = new DirectFieldAccessor(handler);

		Object executor = accessor.getPropertyValue("executor");
		DirectFieldAccessor executorAccessor = new DirectFieldAccessor(executor);

		Object procedureParameters = executorAccessor.getPropertyValue("procedureParameters");
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
		assertThat(parameter4.getValue()).isEqualTo(Integer.valueOf(30));

		assertThat(parameter1.getExpression()).isNull();
		assertThat(parameter2.getExpression()).isNull();
		assertThat(parameter3.getExpression()).isEqualTo("payload.username");
		assertThat(parameter4.getExpression()).isNull();

	}

	@SuppressWarnings("unchecked")
	@Test
	public void testSqlParametersAreSet() {
		setUp("basicStoredProcOutboundChannelAdapterTest.xml", getClass());

		DirectFieldAccessor accessor = new DirectFieldAccessor(this.consumer);
		Object handler = accessor.getPropertyValue("handler");
		accessor = new DirectFieldAccessor(handler);

		Object executor = accessor.getPropertyValue("executor");
		DirectFieldAccessor executorAccessor = new DirectFieldAccessor(executor);

		Object sqlParameters = executorAccessor.getPropertyValue("sqlParameters");

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

		assertThat(parameter1 instanceof SqlParameter).isTrue();
		assertThat(parameter2 instanceof SqlOutParameter).isTrue();
		assertThat(parameter3 instanceof SqlInOutParameter).isTrue();
		assertThat(parameter4 instanceof SqlParameter).isTrue();

	}

	@Test
	public void adviceCalled() {
		setUp("advisedStoredProcOutboundChannelAdapterTest.xml", getClass());

		MessageHandler handler = TestUtils.getPropertyValue(this.consumer, "handler");
		handler.handleMessage(new GenericMessage<>("foo"));
		assertThat(adviceCalled).isEqualTo(1);
	}

	@AfterEach
	public void tearDown() {
		if (context != null) {
			context.close();
		}
	}

	public void setUp(String name, Class<?> cls) {
		context = new ClassPathXmlApplicationContext(name, cls);
		consumer = this.context.getBean("storedProcedureOutboundChannelAdapter", EventDrivenConsumer.class);
	}

	public static class FooAdvice extends AbstractRequestHandlerAdvice {

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) {
			adviceCalled++;
			return null;
		}

	}

}
