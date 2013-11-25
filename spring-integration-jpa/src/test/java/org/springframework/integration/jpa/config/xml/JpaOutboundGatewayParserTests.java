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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.junit.After;
import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.common.LiteralExpression;
import org.springframework.expression.spel.standard.SpelExpression;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.handler.ReplyRequiredException;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.jpa.core.JpaExecutor;
import org.springframework.integration.jpa.core.JpaOperations;
import org.springframework.integration.jpa.outbound.JpaOutboundGateway;
import org.springframework.integration.jpa.support.OutboundGatewayType;
import org.springframework.integration.jpa.support.PersistMode;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;


/**
 * @author Gunnar Hillert
 * @author Amol Nayak
 * @author Gary Russell
 * @author Artem Bilan
 * @since 2.2
 *
 */
public class JpaOutboundGatewayParserTests extends AbstractRequestHandlerAdvice {

	private ConfigurableApplicationContext context;

	private EventDrivenConsumer consumer;

	@Test
	public void testRetrievingJpaOutboundGatewayParser() throws Exception {
		setUp("JpaOutboundGatewayParserTests.xml", getClass(), "retrievingJpaOutboundGateway");
		final AbstractMessageChannel inputChannel = TestUtils.getPropertyValue(this.consumer, "inputChannel", AbstractMessageChannel.class);
		assertEquals("in", inputChannel.getComponentName());
		final JpaOutboundGateway jpaOutboundGateway = TestUtils.getPropertyValue(this.consumer, "handler", JpaOutboundGateway.class);
		final OutboundGatewayType gatewayType = TestUtils.getPropertyValue(jpaOutboundGateway, "gatewayType", OutboundGatewayType.class);
		assertEquals(OutboundGatewayType.RETRIEVING, gatewayType);
		long sendTimeout = TestUtils.getPropertyValue(jpaOutboundGateway, "messagingTemplate.sendTimeout", Long.class);
		assertEquals(100, sendTimeout);
		assertFalse(TestUtils.getPropertyValue(jpaOutboundGateway, "requiresReply", Boolean.class));
		final JpaExecutor jpaExecutor = TestUtils.getPropertyValue(this.consumer, "handler.jpaExecutor", JpaExecutor.class);
		assertNotNull(jpaExecutor);
		final Class<?> entityClass = TestUtils.getPropertyValue(jpaExecutor, "entityClass", Class.class);
		assertEquals("org.springframework.integration.jpa.test.entity.StudentDomain", entityClass.getName());
		final JpaOperations jpaOperations = TestUtils.getPropertyValue(jpaExecutor, "jpaOperations", JpaOperations.class);
		assertNotNull(jpaOperations);
		assertTrue(TestUtils.getPropertyValue(jpaExecutor, "expectSingleResult", Boolean.class));
		final LiteralExpression maxResultsExpression =
				TestUtils.getPropertyValue(jpaExecutor, "maxResultsExpression", LiteralExpression.class);
		assertNotNull(maxResultsExpression);
		assertEquals("55", TestUtils.getPropertyValue(maxResultsExpression, "literalValue"));

		assertTrue(TestUtils.getPropertyValue(jpaExecutor, "deleteAfterPoll", Boolean.class));
		assertTrue(TestUtils.getPropertyValue(jpaExecutor, "flush", Boolean.class));
	}

	@Test
	public void testRetrievingJpaOutboundGatewayParserWithFirstResult() throws Exception {
		setUp("JpaOutboundGatewayParserTests.xml", getClass(), "retrievingJpaOutboundGatewayWithFirstResult");
		final JpaOutboundGateway jpaOutboundGateway = TestUtils.getPropertyValue(this.consumer, "handler", JpaOutboundGateway.class);
		Expression firstResultExpression =
			TestUtils.getPropertyValue(jpaOutboundGateway, "jpaExecutor.firstResultExpression", Expression.class);
		assertNotNull(firstResultExpression);
		assertEquals(LiteralExpression.class, firstResultExpression.getClass());
		assertEquals("1", TestUtils.getPropertyValue(firstResultExpression, "literalValue", String.class));
	}

	@Test
	public void testRetrievingJpaOutboundGatewayParserWithFirstResultExpression() throws Exception {
		setUp("JpaOutboundGatewayParserTests.xml", getClass(), "retrievingJpaOutboundGatewayWithFirstResultExpression");
		final JpaOutboundGateway jpaOutboundGateway = TestUtils.getPropertyValue(this.consumer, "handler", JpaOutboundGateway.class);
		Expression firstResultExpression =
			TestUtils.getPropertyValue(jpaOutboundGateway, "jpaExecutor.firstResultExpression", Expression.class);
		assertNotNull(firstResultExpression);
		assertEquals(SpelExpression.class, firstResultExpression.getClass());
		assertEquals("header['firstResult']", TestUtils.getPropertyValue(firstResultExpression, "expression", String.class));
	}

	@Test
	public void testRetrievingJpaOutboundGatewayParserWithMaxResultExpression() throws Exception {
		setUp("JpaOutboundGatewayParserTests.xml", getClass(), "retrievingJpaOutboundGatewayWithMaxResultExpression");
		final JpaOutboundGateway jpaOutboundGateway = TestUtils.getPropertyValue(this.consumer, "handler", JpaOutboundGateway.class);
		Expression maxNumberOfResultExpression =
			TestUtils.getPropertyValue(jpaOutboundGateway, "jpaExecutor.maxResultsExpression", Expression.class);
		assertNotNull(maxNumberOfResultExpression);
		assertEquals(SpelExpression.class, maxNumberOfResultExpression.getClass());
		assertEquals("header['maxResults']", TestUtils.getPropertyValue(maxNumberOfResultExpression, "expression", String.class));
	}

	@Test
	public void testUpdatingJpaOutboundGatewayParser() throws Exception {
		setUp("JpaOutboundGatewayParserTests.xml", getClass(), "updatingJpaOutboundGateway");


		final AbstractMessageChannel inputChannel = TestUtils.getPropertyValue(this.consumer, "inputChannel", AbstractMessageChannel.class);

		assertEquals("in", inputChannel.getComponentName());

		final JpaOutboundGateway jpaOutboundGateway = TestUtils.getPropertyValue(this.consumer, "handler", JpaOutboundGateway.class);

		final OutboundGatewayType gatewayType = TestUtils.getPropertyValue(jpaOutboundGateway, "gatewayType", OutboundGatewayType.class);

		assertEquals(OutboundGatewayType.UPDATING, gatewayType);

		long sendTimeout = TestUtils.getPropertyValue(jpaOutboundGateway, "messagingTemplate.sendTimeout", Long.class);

		assertEquals(100, sendTimeout);

		assertFalse(TestUtils.getPropertyValue(jpaOutboundGateway, "requiresReply", Boolean.class));

		final JpaExecutor jpaExecutor = TestUtils.getPropertyValue(this.consumer, "handler.jpaExecutor", JpaExecutor.class);

		assertNotNull(jpaExecutor);

		final Class<?> entityClass = TestUtils.getPropertyValue(jpaExecutor, "entityClass", Class.class);

		assertEquals("org.springframework.integration.jpa.test.entity.StudentDomain", entityClass.getName());

		final JpaOperations jpaOperations = TestUtils.getPropertyValue(jpaExecutor, "jpaOperations", JpaOperations.class);

		assertNotNull(jpaOperations);

		final Boolean usePayloadAsParameterSource = TestUtils.getPropertyValue(jpaExecutor, "usePayloadAsParameterSource", Boolean.class);

		assertTrue(usePayloadAsParameterSource);

		final Integer order = TestUtils.getPropertyValue(jpaOutboundGateway, "order", Integer.class);

		assertEquals(Integer.valueOf(2), order);

		final PersistMode persistMode = TestUtils.getPropertyValue(jpaExecutor, "persistMode", PersistMode.class);

		assertEquals(PersistMode.PERSIST, persistMode);

		assertEquals(Integer.valueOf(100), TestUtils.getPropertyValue(jpaExecutor, "flushSize", Integer.class));
		assertTrue(TestUtils.getPropertyValue(jpaExecutor, "clearOnFlush", Boolean.class));
	}

	@Test
	public void advised() throws Throwable {
		setUp("JpaOutboundGatewayParserTests.xml", getClass(), "advised");

		MessageHandler jpaOutboundGateway = context.getBean("advised.handler", MessageHandler.class);
		FooAdvice advice = context.getBean("jpaFooAdvice", FooAdvice.class);
		assertTrue(AopUtils.isAopProxy(jpaOutboundGateway));

		try {
			jpaOutboundGateway.handleMessage(new GenericMessage<String>("foo"));
			fail("expected ReplyRequiredException");
		}
		catch (MessagingException e) {
			assertTrue(e instanceof ReplyRequiredException);
		}

		Mockito.verify(advice).doInvoke(Mockito.any(ExecutionCallback.class), Mockito.any(Object.class), Mockito.any(Message.class));
	}

	@Test
	public void testJpaExecutorBeanIdNaming() throws Exception {

		this.context = new ClassPathXmlApplicationContext("JpaOutboundGatewayParserTests.xml", getClass());

		assertNotNull(context.getBean("retrievingJpaOutboundGateway.jpaExecutor", JpaExecutor.class));
		assertNotNull(context.getBean("updatingJpaOutboundGateway.jpaExecutor", JpaExecutor.class));

	}

	@Test
	public void withBothFirstResultAndFirstResultExpressionPresent() {
		try {
			this.context = new ClassPathXmlApplicationContext("JpaInvalidOutboundGatewayParserTests.xml", getClass());
		} catch (BeanDefinitionStoreException e) {
			assertTrue(e.getMessage().startsWith("Configuration problem: Only one of 'first-result' or 'first-result-expression' is allowed"));
			return;
		}
		fail("BeanDefinitionStoreException expected.");

	}

	@After
	public void tearDown() {
		if (context != null) {
			context.close();
		}
	}

	public void setUp(String name, Class<?> cls, String gatewayId) {
		context    = new ClassPathXmlApplicationContext(name, cls);
		consumer   = this.context.getBean(gatewayId, EventDrivenConsumer.class);
	}

	@Override
	protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) throws Exception {
		// Workaround for access to protected AbstractRequestHandlerAdvice.ExecutionCallback
		return null;
	}

	public static class FooAdvice extends AbstractRequestHandlerAdvice {

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) throws Exception {
			return null;
		}

	}
}
