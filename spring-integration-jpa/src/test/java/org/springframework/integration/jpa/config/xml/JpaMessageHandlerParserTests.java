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
package org.springframework.integration.jpa.config.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Proxy;
import java.util.List;

import org.junit.After;
import org.junit.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.jpa.core.JpaExecutor;
import org.springframework.integration.jpa.core.JpaOperations;
import org.springframework.integration.jpa.support.JpaParameter;
import org.springframework.integration.jpa.support.PersistMode;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.integration.test.util.TestUtils;

/**
 *
 * @author Gunnar Hillert
 * @author Artem Bilan
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


		final AbstractMessageChannel inputChannel = TestUtils.getPropertyValue(this.consumer, "inputChannel", AbstractMessageChannel.class);

		assertEquals("target", inputChannel.getComponentName());

		final JpaExecutor jpaExecutor = TestUtils.getPropertyValue(this.consumer, "handler.jpaExecutor", JpaExecutor.class);

		assertNotNull(jpaExecutor);

		final String query = TestUtils.getPropertyValue(jpaExecutor, "jpaQuery", String.class);

		assertEquals("from Student", query);

		final JpaOperations jpaOperations = TestUtils.getPropertyValue(jpaExecutor, "jpaOperations", JpaOperations.class);

		assertNotNull(jpaOperations);

		final PersistMode persistMode = TestUtils.getPropertyValue(jpaExecutor, "persistMode", PersistMode.class);

		assertEquals(PersistMode.PERSIST, persistMode);

		@SuppressWarnings("unchecked")
		List<JpaParameter> jpaParameters = TestUtils.getPropertyValue(jpaExecutor, "jpaParameters", List.class);

		assertNotNull(jpaParameters);
		assertTrue(jpaParameters.size() == 3);

		assertEquals(Integer.valueOf(10), TestUtils.getPropertyValue(jpaExecutor, "flushSize", Integer.class));
		assertTrue(TestUtils.getPropertyValue(jpaExecutor, "clearOnFlush", Boolean.class));
	}

	@Test
	public void advised() throws Exception {
		setUp("JpaMessageHandlerParserTests.xml", getClass());

		EventDrivenConsumer consumer = this.context.getBean("advised", EventDrivenConsumer.class);

		final AbstractMessageChannel inputChannel = TestUtils.getPropertyValue(consumer, "inputChannel", AbstractMessageChannel.class);

		assertEquals("target", inputChannel.getComponentName());

		final MessageHandler handler = TestUtils.getPropertyValue(consumer, "handler", MessageHandler.class);

		adviceCalled = 0;

		handler.handleMessage(new GenericMessage<String>("foo"));

		assertEquals(1, adviceCalled);
	}

	/*
	 * Tests that an already advised handler (tx) gets the request handler advice added to its chain.
	 */
	@Test
	public void advisedAndTransactional() throws Exception {
		setUp("JpaMessageHandlerParserTests.xml", getClass());

		EventDrivenConsumer consumer = this.context.getBean("advisedAndTransactional", EventDrivenConsumer.class);

		final AbstractMessageChannel inputChannel = TestUtils.getPropertyValue(consumer, "inputChannel", AbstractMessageChannel.class);

		assertEquals("target", inputChannel.getComponentName());

		final MessageHandler handler = TestUtils.getPropertyValue(consumer, "handler", MessageHandler.class);

		adviceCalled = 0;

		handler.handleMessage(new GenericMessage<String>("foo"));

		assertEquals(1, adviceCalled);
	}

	@Test
	public void testJpaMessageHandlerParserWithEntityManagerFactory() throws Exception {
		setUp("JpaMessageHandlerParserTestsWithEmFactory.xml", getClass());


		final AbstractMessageChannel inputChannel = TestUtils.getPropertyValue(this.consumer, "inputChannel", AbstractMessageChannel.class);

		assertEquals("target", inputChannel.getComponentName());

		final JpaExecutor jpaExecutor = TestUtils.getPropertyValue(this.consumer, "handler.jpaExecutor", JpaExecutor.class);

		assertNotNull(jpaExecutor);

		final String query = TestUtils.getPropertyValue(jpaExecutor, "jpaQuery", String.class);

		assertEquals("select student from Student student", query);

		final JpaOperations jpaOperations = TestUtils.getPropertyValue(jpaExecutor, "jpaOperations", JpaOperations.class);

		assertNotNull(jpaOperations);

		final PersistMode persistMode = TestUtils.getPropertyValue(jpaExecutor, "persistMode", PersistMode.class);

		assertEquals(PersistMode.PERSIST, persistMode);

		@SuppressWarnings("unchecked")
		List<JpaParameter> jpaParameters = TestUtils.getPropertyValue(jpaExecutor, "jpaParameters", List.class);

		assertNotNull(jpaParameters);
		assertTrue(jpaParameters.size() == 3);

}

	@SuppressWarnings("unchecked")
	@Test
	public void testProcedurepParametersAreSet() throws Exception {
		setUp("JpaMessageHandlerParserTestsWithEmFactory.xml", getClass());

		final JpaExecutor jpaExecutor = TestUtils.getPropertyValue(this.consumer, "handler.jpaExecutor", JpaExecutor.class);
		final List<JpaParameter> jpaParameters = TestUtils.getPropertyValue(jpaExecutor, "jpaParameters", List.class);

		assertTrue(jpaParameters.size() == 3);

		JpaParameter parameter1 = jpaParameters.get(0);
		JpaParameter parameter2 = jpaParameters.get(1);
		JpaParameter parameter3 = jpaParameters.get(2);

		assertEquals("firstName",       parameter1.getName());
		assertEquals("firstaName",      parameter2.getName());
		assertEquals("updatedDateTime", parameter3.getName());

		assertEquals("kenny",    parameter1.getValue());
		assertEquals("cartman",  parameter2.getValue());
		assertNull(parameter3.getValue());

		assertNull(parameter1.getExpression());
		assertNull(parameter2.getExpression());
		assertEquals("new java.util.Date()", parameter3.getExpression());

	}

	@Test
	public void testTransactionalSettings() throws Exception {

		setUp("JpaMessageHandlerTransactionalParserTests.xml", getClass());

		final Proxy proxy = TestUtils.getPropertyValue(this.consumer, "handler", Proxy.class);
		assertNotNull(proxy);

	}

	@Test
	public void testJpaExecutorBeanIdNaming() throws Exception {

		setUp("JpaMessageHandlerParserTestsWithEmFactory.xml", getClass());

		assertNotNull(context.getBean("jpaOutboundChannelAdapter.jpaExecutor", JpaExecutor.class));

	}

	@After
	public void tearDown(){
		if(context != null){
			context.close();
		}
	}

	public void setUp(String name, Class<?> cls){
		context    = new ClassPathXmlApplicationContext(name, cls);
		consumer   = this.context.getBean("jpaOutboundChannelAdapter", EventDrivenConsumer.class);
	}

	public static class FooAdvice extends AbstractRequestHandlerAdvice {

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) throws Exception {
			adviceCalled++;
			return null;
		}

	}
}
