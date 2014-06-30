/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.integration.jms.config;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.lang.reflect.Method;
import java.util.Properties;

import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.Queue;
import javax.jms.Session;

import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.NotReadablePropertyException;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.expression.Expression;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.endpoint.PollingConsumer;
import org.springframework.integration.handler.ExpressionEvaluatingMessageProcessor;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.jms.JmsOutboundGateway;
import org.springframework.integration.jms.StubMessageConverter;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.support.converter.MessageConverter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Jonas Partner
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Gary Russell
 */
public class JmsOutboundGatewayParserTests {

	private static volatile int adviceCalled;

	@Test
	public void testWithDeliveryPersistentAttribute(){
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsOutboundGatewayWithDeliveryPersistent.xml", this.getClass());
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context.getBean("jmsGateway");
		DirectFieldAccessor accessor = new DirectFieldAccessor(endpoint);
		JmsOutboundGateway gateway = (JmsOutboundGateway) accessor.getPropertyValue("handler");
		accessor = new DirectFieldAccessor(gateway);
		int deliveryMode = (Integer)accessor.getPropertyValue("deliveryMode");
		assertEquals(DeliveryMode.PERSISTENT, deliveryMode);
		DefaultMessageListenerContainer container = TestUtils.getPropertyValue(gateway, "replyContainer",
				DefaultMessageListenerContainer.class);
		assertEquals(4, TestUtils.getPropertyValue(container, "concurrentConsumers"));
		assertEquals(5, TestUtils.getPropertyValue(container, "maxConcurrentConsumers"));
		assertEquals(10, TestUtils.getPropertyValue(container, "maxMessagesPerTask"));
		assertEquals(2000L, TestUtils.getPropertyValue(container, "receiveTimeout"));
		Object recoveryInterval;
		try {
			recoveryInterval = TestUtils.getPropertyValue(container, "recoveryInterval");
		}
		catch (NotReadablePropertyException e) {
			recoveryInterval = TestUtils.getPropertyValue(container, "backOff.interval");
		}
		assertEquals(10000L, recoveryInterval);

		assertEquals(7, TestUtils.getPropertyValue(container, "idleConsumerLimit"));
		assertEquals(2, TestUtils.getPropertyValue(container, "idleTaskExecutionLimit"));
		assertEquals(3, TestUtils.getPropertyValue(container, "cacheLevel"));
		assertTrue(container.isSessionTransacted());
		assertSame(context.getBean("exec"), TestUtils.getPropertyValue(container, "taskExecutor"));
	}

	@Test
	public void testAdvised(){
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsOutboundGatewayWithDeliveryPersistent.xml", this.getClass());
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context.getBean("advised");
		JmsOutboundGateway gateway = TestUtils.getPropertyValue(endpoint, "handler", JmsOutboundGateway.class);
		gateway.handleMessage(new GenericMessage<String>("foo"));
		assertEquals(1, adviceCalled);
		assertEquals(3, TestUtils.getPropertyValue(gateway, "replyContainer.sessionAcknowledgeMode"));
	}

	@Test
	public void testDefault(){
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsOutboundGatewayWithConverter.xml", this.getClass());
		PollingConsumer endpoint = (PollingConsumer) context.getBean("jmsGateway");
		DirectFieldAccessor accessor = new DirectFieldAccessor(endpoint);
		JmsOutboundGateway gateway = (JmsOutboundGateway) accessor.getPropertyValue("handler");
		accessor = new DirectFieldAccessor(gateway);
		MessageConverter converter = (MessageConverter)accessor.getPropertyValue("messageConverter");
		assertTrue("Wrong message converter", converter instanceof StubMessageConverter);
	}

	@Test
	public void gatewayWithOrder() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsOutboundGatewayWithOrder.xml", this.getClass());
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context.getBean("jmsGateway");
		DirectFieldAccessor accessor = new DirectFieldAccessor(
				new DirectFieldAccessor(endpoint).getPropertyValue("handler"));
		Object order = accessor.getPropertyValue("order");
		assertEquals(99, order);
		assertEquals(Boolean.TRUE, accessor.getPropertyValue("requiresReply"));
	}

	@Test
	public void gatewayWithDest() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsOutboundGatewayReplyDestOptions.xml", this.getClass());
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context.getBean("jmsGatewayDest");
		DirectFieldAccessor accessor = new DirectFieldAccessor(endpoint);
		JmsOutboundGateway gateway = (JmsOutboundGateway) accessor.getPropertyValue("handler");
		accessor = new DirectFieldAccessor(gateway);
		assertSame(context.getBean("replyQueue"), accessor.getPropertyValue("replyDestination"));
	}

	@Test
	public void gatewayWithDestName() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsOutboundGatewayReplyDestOptions.xml", this.getClass());
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context.getBean("jmsGatewayDestName");
		DirectFieldAccessor accessor = new DirectFieldAccessor(endpoint);
		JmsOutboundGateway gateway = (JmsOutboundGateway) accessor.getPropertyValue("handler");
		accessor = new DirectFieldAccessor(gateway);
		assertEquals("replyQueueName", accessor.getPropertyValue("replyDestinationName"));
	}

	@Test
	public void gatewayWithDestExpression() throws Exception {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsOutboundGatewayReplyDestOptions.xml", this.getClass());
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context.getBean("jmsGatewayDestExpression");
		DirectFieldAccessor accessor = new DirectFieldAccessor(endpoint);
		JmsOutboundGateway gateway = (JmsOutboundGateway) accessor.getPropertyValue("handler");
		ExpressionEvaluatingMessageProcessor<?> processor = TestUtils.getPropertyValue(gateway, "replyDestinationExpressionProcessor",
				ExpressionEvaluatingMessageProcessor.class);
		Expression expression = TestUtils.getPropertyValue(gateway, "replyDestinationExpressionProcessor.expression",
				Expression.class);
		assertEquals("payload", expression.getExpressionString());
		Message<?> message = MessageBuilder.withPayload("foo").build();
		assertEquals("foo", processor.processMessage(message));

		Method method = JmsOutboundGateway.class.getDeclaredMethod("determineReplyDestination", Message.class, Session.class);
		method.setAccessible(true);

		Session session = mock(Session.class);
		Queue queue = mock(Queue.class);
		when(session.createQueue("foo")).thenReturn(queue);
		Destination replyQ = (Destination) method.invoke(gateway, message, session);
		assertSame(queue, replyQ);
	}

	@Test
	public void gatewayWithDestBeanRefExpression() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsOutboundGatewayReplyDestOptions.xml", this.getClass());
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context.getBean("jmsGatewayDestExpressionBeanRef");
		DirectFieldAccessor accessor = new DirectFieldAccessor(endpoint);
		JmsOutboundGateway gateway = (JmsOutboundGateway) accessor.getPropertyValue("handler");
		ExpressionEvaluatingMessageProcessor<?> processor = TestUtils.getPropertyValue(gateway, "replyDestinationExpressionProcessor",
				ExpressionEvaluatingMessageProcessor.class);
		Expression expression = TestUtils.getPropertyValue(gateway, "replyDestinationExpressionProcessor.expression",
				Expression.class);
		assertEquals("@replyQueue", expression.getExpressionString());
		assertSame(context.getBean("replyQueue"), processor.processMessage(null));
	}

	@Test
	public void gatewayWithDestAndDestExpression() {
		try {
			new ClassPathXmlApplicationContext(
				"jmsOutboundGatewayReplyDestOptions-fail.xml", this.getClass());
			fail("Exception expected");
		}
		catch (BeanDefinitionParsingException e) {
			assertTrue(e.getMessage().startsWith("Configuration problem: Only one of the " +
					"'replyQueue', 'reply-destination-name', or 'reply-destination-expression' attributes is allowed."));
		}
	}

	@Test
	public void gatewayMaintainsReplyChannelAndInboundHistory() {
		ActiveMqTestUtils.prepare();
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"gatewayMaintainsReplyChannel.xml", this.getClass());
		SampleGateway gateway = context.getBean("gateway", SampleGateway.class);
		SubscribableChannel jmsInput = context.getBean("jmsInput", SubscribableChannel.class);
		MessageHandler handler = new MessageHandler() {
			public void handleMessage(Message<?> message) throws MessagingException {
				MessageHistory history = MessageHistory.read(message);
				assertNotNull(history);
				Properties componentHistoryRecord = TestUtils.locateComponentInHistory(history, "inboundGateway", 0);
				assertNotNull(componentHistoryRecord);
				assertEquals("jms:inbound-gateway", componentHistoryRecord.get("type"));
			    MessagingTemplate messagingTemplate = new MessagingTemplate();
			    messagingTemplate.setDefaultDestination((MessageChannel)message.getHeaders().getReplyChannel());
			    messagingTemplate.send(message);
			}
		};
		handler = spy(handler);
		jmsInput.subscribe(handler);
		String result = gateway.echo("hello");
		verify(handler, times(1)).handleMessage(Mockito.any(Message.class));
		assertEquals("hello", result);
	}

	@Test
	public void gatewayWithDefaultPubSubDomain() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsOutboundGatewayWithPubSubSettings.xml", this.getClass());
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context.getBean("defaultGateway");
		DirectFieldAccessor accessor = new DirectFieldAccessor(
				new DirectFieldAccessor(endpoint).getPropertyValue("handler"));
		assertFalse((Boolean) accessor.getPropertyValue("requestPubSubDomain"));
		assertFalse((Boolean) accessor.getPropertyValue("replyPubSubDomain"));
	}

	@Test
	public void gatewayWithExplicitPubSubDomainTrue() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsOutboundGatewayWithPubSubSettings.xml", this.getClass());
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context.getBean("pubSubDomainGateway");
		DirectFieldAccessor accessor = new DirectFieldAccessor(
				new DirectFieldAccessor(endpoint).getPropertyValue("handler"));
		assertTrue((Boolean) accessor.getPropertyValue("requestPubSubDomain"));
		assertTrue((Boolean) accessor.getPropertyValue("replyPubSubDomain"));
	}


	public static interface SampleGateway{
		public String echo(String value);
	}

	public static class SampleService{
		public String echo(String value){
			return value.toUpperCase();
		}
	}

	public static class FooAdvice extends AbstractRequestHandlerAdvice {

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) throws Exception {
			adviceCalled++;
			return null;
		}

	}
}
