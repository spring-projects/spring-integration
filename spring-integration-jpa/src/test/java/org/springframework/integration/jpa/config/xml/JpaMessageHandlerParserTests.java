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

package org.springframework.integration.jpa.config.xml;

import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import org.springframework.aop.support.AopUtils;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.jpa.core.JpaExecutor;
import org.springframework.integration.jpa.core.JpaOperations;
import org.springframework.integration.jpa.support.JpaParameter;
import org.springframework.integration.jpa.support.PersistMode;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Glenn Renfro
 *
 * @since 2.2
 *
 */
public class JpaMessageHandlerParserTests {

	private ConfigurableApplicationContext context;

	private EventDrivenConsumer consumer;

	private static volatile int adviceCalled;

	@Test
	public void testJpaMessageHandlerParser() throws Exception {
		setUp("JpaMessageHandlerParserTests.xml", getClass());

		final AbstractMessageChannel inputChannel = TestUtils.<AbstractMessageChannel>getPropertyValue(this.consumer, "inputChannel");

		assertThat(inputChannel.getComponentName()).isEqualTo("target");

		final JpaExecutor jpaExecutor = TestUtils.<JpaExecutor>getPropertyValue(this.consumer, "handler.jpaExecutor");

		assertThat(jpaExecutor).isNotNull();

		final String query = TestUtils.<String>getPropertyValue(jpaExecutor, "jpaQuery");

		assertThat(query).isEqualTo("from Student");

		final JpaOperations jpaOperations = TestUtils.<JpaOperations>getPropertyValue(jpaExecutor, "jpaOperations");

		assertThat(jpaOperations).isNotNull();

		final PersistMode persistMode = TestUtils.<PersistMode>getPropertyValue(jpaExecutor, "persistMode");

		assertThat(persistMode).isEqualTo(PersistMode.PERSIST);

		@SuppressWarnings("unchecked")
		List<JpaParameter> jpaParameters = TestUtils.getPropertyValue(jpaExecutor, "jpaParameters");

		assertThat(jpaParameters).isNotNull();
		assertThat(jpaParameters.size() == 3).isTrue();

		assertThat(TestUtils.<Integer>getPropertyValue(jpaExecutor, "flushSize")).isEqualTo(Integer.valueOf(10));
		assertThat(TestUtils.<Boolean>getPropertyValue(jpaExecutor, "clearOnFlush")).isTrue();
	}

	@Test
	public void advised() throws Exception {
		setUp("JpaMessageHandlerParserTests.xml", getClass());

		EventDrivenConsumer consumer = this.context.getBean("advised", EventDrivenConsumer.class);

		final AbstractMessageChannel inputChannel = TestUtils.<AbstractMessageChannel>getPropertyValue(consumer, "inputChannel");

		assertThat(inputChannel.getComponentName()).isEqualTo("target");

		final MessageHandler handler = TestUtils.<MessageHandler>getPropertyValue(consumer, "handler");

		adviceCalled = 0;

		handler.handleMessage(new GenericMessage<String>("foo"));

		assertThat(adviceCalled).isEqualTo(1);
	}

	/*
	 * Tests that an already advised handler (tx) gets the request handler advice added to its chain.
	 */
	@Test
	public void advisedAndTransactional() throws Exception {
		setUp("JpaMessageHandlerParserTests.xml", getClass());

		EventDrivenConsumer consumer = this.context.getBean("advisedAndTransactional", EventDrivenConsumer.class);

		final AbstractMessageChannel inputChannel = TestUtils.<AbstractMessageChannel>getPropertyValue(consumer, "inputChannel");

		assertThat(inputChannel.getComponentName()).isEqualTo("target");

		final MessageHandler handler = TestUtils.<MessageHandler>getPropertyValue(consumer, "handler");

		adviceCalled = 0;

		handler.handleMessage(new GenericMessage<String>("foo"));

		assertThat(adviceCalled).isEqualTo(1);
	}

	@Test
	public void testJpaMessageHandlerParserWithEntityManagerFactory() throws Exception {
		setUp("JpaMessageHandlerParserTestsWithEmFactory.xml", getClass());

		final AbstractMessageChannel inputChannel = TestUtils.<AbstractMessageChannel>getPropertyValue(this.consumer, "inputChannel");

		assertThat(inputChannel.getComponentName()).isEqualTo("target");

		final JpaExecutor jpaExecutor = TestUtils.<JpaExecutor>getPropertyValue(this.consumer, "handler.jpaExecutor");

		assertThat(jpaExecutor).isNotNull();

		final String query = TestUtils.<String>getPropertyValue(jpaExecutor, "jpaQuery");

		assertThat(query).isEqualTo("select student from Student student");

		final JpaOperations jpaOperations = TestUtils.<JpaOperations>getPropertyValue(jpaExecutor, "jpaOperations");

		assertThat(jpaOperations).isNotNull();

		final PersistMode persistMode = TestUtils.<PersistMode>getPropertyValue(jpaExecutor, "persistMode");

		assertThat(persistMode).isEqualTo(PersistMode.PERSIST);

		@SuppressWarnings("unchecked")
		List<JpaParameter> jpaParameters = TestUtils.getPropertyValue(jpaExecutor, "jpaParameters");

		assertThat(jpaParameters).isNotNull();
		assertThat(jpaParameters.size() == 3).isTrue();

	}

	@SuppressWarnings("unchecked")
	@Test
	public void testProcedureParametersAreSet() throws Exception {
		setUp("JpaMessageHandlerParserTestsWithEmFactory.xml", getClass());

		final JpaExecutor jpaExecutor = TestUtils.<JpaExecutor>getPropertyValue(this.consumer, "handler.jpaExecutor");
		final List<JpaParameter> jpaParameters = TestUtils.getPropertyValue(jpaExecutor, "jpaParameters");

		assertThat(jpaParameters.size() == 3).isTrue();

		JpaParameter parameter1 = jpaParameters.get(0);
		JpaParameter parameter2 = jpaParameters.get(1);
		JpaParameter parameter3 = jpaParameters.get(2);

		assertThat(parameter1.getName()).isEqualTo("firstName");
		assertThat(parameter2.getName()).isEqualTo("firstaName");
		assertThat(parameter3.getName()).isEqualTo("updatedDateTime");

		assertThat(parameter1.getValue()).isEqualTo("kenny");
		assertThat(parameter2.getValue()).isEqualTo("cartman");
		assertThat(parameter3.getValue()).isNull();

		assertThat(parameter1.getExpression()).isNull();
		assertThat(parameter2.getExpression()).isNull();
		assertThat(parameter3.getExpression()).isEqualTo("new java.util.Date()");

	}

	@Test
	public void testTransactionalSettings() throws Exception {

		setUp("JpaMessageHandlerTransactionalParserTests.xml", getClass());

		AbstractReplyProducingMessageHandler.RequestHandler handler =
				TestUtils.getPropertyValue(this.consumer, "handler.advisedRequestHandler");
		assertThat(handler).isNotNull();
		assertThat(AopUtils.isAopProxy(handler)).isTrue();
	}

	@Test
	public void testJpaExecutorBeanIdNaming() throws Exception {

		setUp("JpaMessageHandlerParserTestsWithEmFactory.xml", getClass());

		assertThat(context.getBean("jpaOutboundChannelAdapter.jpaExecutor", JpaExecutor.class)).isNotNull();

	}

	@AfterEach
	public void tearDown() {
		if (context != null) {
			context.close();
		}
	}

	public void setUp(String name, Class<?> cls) {
		context = new ClassPathXmlApplicationContext(name, cls);
		consumer = this.context.getBean("jpaOutboundChannelAdapter", EventDrivenConsumer.class);
	}

	public static class FooAdvice extends AbstractRequestHandlerAdvice {

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) {
			adviceCalled++;
			return null;
		}

	}

}
