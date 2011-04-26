/*
 * Copyright 2002-2011 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Properties;

import javax.jms.DeliveryMode;

import org.junit.Test;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.jms.JmsMessageDrivenEndpoint;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.jms.connection.JmsTransactionManager;
import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.jms.support.destination.JmsDestinationAccessor;

/**
 * @author Mark Fisher
 */
public class JmsInboundGatewayParserTests {

	@Test
	public void testGatewayWithConnectionFactoryAndDestination() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsGatewayWithConnectionFactoryAndDestination.xml", this.getClass());
		PollableChannel channel = (PollableChannel) context.getBean("requestChannel");
		JmsMessageDrivenEndpoint gateway = (JmsMessageDrivenEndpoint) context.getBean("jmsGateway");
		assertEquals(JmsMessageDrivenEndpoint.class, gateway.getClass());
		context.start();
		Message<?> message = channel.receive(3000);
		MessageHistory history = MessageHistory.read(message);
		assertNotNull(history);
		Properties componentHistoryRecord = TestUtils.locateComponentInHistory(history, "jmsGateway", 0);
		assertNotNull(componentHistoryRecord);
		assertEquals("jms:inbound-gateway", componentHistoryRecord.get("type"));
		assertNotNull("message should not be null", message);
		assertEquals("message-driven-test", message.getPayload());
		context.stop();
	}

	@Test
	public void testGatewayWithConnectionFactoryAndDestinationName() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsGatewayWithConnectionFactoryAndDestinationName.xml", this.getClass());
		PollableChannel channel = (PollableChannel) context.getBean("requestChannel");
		JmsMessageDrivenEndpoint gateway = (JmsMessageDrivenEndpoint) context.getBean("jmsGateway");
		assertEquals(JmsMessageDrivenEndpoint.class, gateway.getClass());
		context.start();
		Message<?> message = channel.receive(3000);
		assertNotNull("message should not be null", message);
		assertEquals("message-driven-test", message.getPayload());
		context.stop();
	}

	@Test
	public void testGatewayWithMessageConverter() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsGatewayWithMessageConverter.xml", this.getClass());
		PollableChannel channel = (PollableChannel) context.getBean("requestChannel");
		JmsMessageDrivenEndpoint gateway = (JmsMessageDrivenEndpoint) context.getBean("jmsGateway");
		assertEquals(JmsMessageDrivenEndpoint.class, gateway.getClass());
		context.start();
		Message<?> message = channel.receive(3000);
		assertNotNull("message should not be null", message);
		assertEquals("converted-test-message", message.getPayload());
		context.stop();
	}

	@Test
	public void testGatewayWithDefaultExtractPayload() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsGatewaysWithExtractPayloadAttributes.xml", this.getClass());
		JmsMessageDrivenEndpoint gateway = (JmsMessageDrivenEndpoint) context.getBean("defaultGateway");
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		accessor = new DirectFieldAccessor(accessor.getPropertyValue("listener"));
		assertEquals(Boolean.TRUE, accessor.getPropertyValue("extractReplyPayload"));
	}

	@Test
	public void testGatewayWithExtractReplyPayloadTrue() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsGatewaysWithExtractPayloadAttributes.xml", this.getClass());
		JmsMessageDrivenEndpoint gateway = (JmsMessageDrivenEndpoint) context.getBean("extractReplyPayloadTrue");
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		accessor = new DirectFieldAccessor(accessor.getPropertyValue("listener"));
		assertEquals(Boolean.TRUE, accessor.getPropertyValue("extractReplyPayload"));
	}

	@Test
	public void testGatewayWithExtractReplyPayloadFalse() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsGatewaysWithExtractPayloadAttributes.xml", this.getClass());
		JmsMessageDrivenEndpoint gateway = (JmsMessageDrivenEndpoint) context.getBean("extractReplyPayloadFalse");
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		accessor = new DirectFieldAccessor(accessor.getPropertyValue("listener"));
		assertEquals(Boolean.FALSE, accessor.getPropertyValue("extractReplyPayload"));
	}

	@Test
	public void testGatewayWithExtractRequestPayloadTrue() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsGatewaysWithExtractPayloadAttributes.xml", this.getClass());
		JmsMessageDrivenEndpoint gateway = (JmsMessageDrivenEndpoint) context.getBean("extractRequestPayloadTrue");
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		accessor = new DirectFieldAccessor(accessor.getPropertyValue("listener"));
		assertEquals(Boolean.TRUE, accessor.getPropertyValue("extractRequestPayload"));
	}

	@Test
	public void testGatewayWithExtractRequestPayloadFalse() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsGatewaysWithExtractPayloadAttributes.xml", this.getClass());
		JmsMessageDrivenEndpoint gateway = (JmsMessageDrivenEndpoint) context.getBean("extractRequestPayloadFalse");
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		accessor = new DirectFieldAccessor(accessor.getPropertyValue("listener"));
		assertEquals(Boolean.FALSE, accessor.getPropertyValue("extractRequestPayload"));
	}

	@Test(expected = BeanDefinitionStoreException.class)
	public void testGatewayWithConnectionFactoryOnly() {
		try {
			new ClassPathXmlApplicationContext("jmsGatewayWithConnectionFactoryOnly.xml", this.getClass());
		}
		catch (BeanDefinitionStoreException e) {
			assertTrue(e.getMessage().contains("request-destination"));
			assertTrue(e.getMessage().contains("request-destination-name"));
			throw e;
		}
	}

	@Test(expected = BeanDefinitionStoreException.class)
	public void testGatewayWithEmptyConnectionFactory() {
		try {
			new ClassPathXmlApplicationContext("jmsGatewayWithEmptyConnectionFactory.xml", this.getClass());
		}
		catch (BeanDefinitionStoreException e) {
			assertTrue(e.getMessage().contains("connection-factory"));
			throw e;
		}
	}

	@Test
	public void testGatewayWithDefaultConnectionFactory() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsGatewayWithDefaultConnectionFactory.xml", this.getClass());
		PollableChannel channel = (PollableChannel) context.getBean("requestChannel");
		JmsMessageDrivenEndpoint gateway = (JmsMessageDrivenEndpoint) context.getBean("jmsGateway");
		assertEquals(JmsMessageDrivenEndpoint.class, gateway.getClass());
		context.start();
		Message<?> message = channel.receive(3000);
		assertNotNull("message should not be null", message);
		assertEquals("message-driven-test", message.getPayload());
		context.stop();
	}

	@Test
	public void testTransactionManagerIsNullByDefault() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsGatewayTransactionManagerTests.xml", this.getClass());
		JmsMessageDrivenEndpoint gateway = (JmsMessageDrivenEndpoint) context.getBean("gatewayWithoutTransactionManager");
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		accessor = new DirectFieldAccessor(accessor.getPropertyValue("listenerContainer"));
		assertNull(accessor.getPropertyValue("transactionManager"));
	}

	@Test
	public void testGatewayWithTransactionManagerReference() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsGatewayTransactionManagerTests.xml", this.getClass());
		JmsMessageDrivenEndpoint gateway = (JmsMessageDrivenEndpoint) context.getBean("gatewayWithTransactionManager");
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		accessor = new DirectFieldAccessor(accessor.getPropertyValue("listenerContainer"));
		Object txManager = accessor.getPropertyValue("transactionManager");
		assertEquals(JmsTransactionManager.class, txManager.getClass());
		assertEquals(context.getBean("txManager"), txManager);
		assertEquals(context.getBean("testConnectionFactory"), ((JmsTransactionManager) txManager).getConnectionFactory());
	}

	@Test
	public void testGatewayWithConcurrentConsumers() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsGatewayWithContainerSettings.xml", this.getClass());
		JmsMessageDrivenEndpoint gateway = (JmsMessageDrivenEndpoint) context.getBean("gatewayWithConcurrentConsumers");
		gateway.start();
		AbstractMessageListenerContainer container = (AbstractMessageListenerContainer)
				new DirectFieldAccessor(gateway).getPropertyValue("listenerContainer");
		assertEquals(3, new DirectFieldAccessor(container).getPropertyValue("concurrentConsumers"));
		gateway.stop();
	}

	@Test
	public void testGatewayWithMaxConcurrentConsumers() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsGatewayWithContainerSettings.xml", this.getClass());
		JmsMessageDrivenEndpoint gateway = (JmsMessageDrivenEndpoint) context.getBean("gatewayWithMaxConcurrentConsumers");
		gateway.start();
		AbstractMessageListenerContainer container = (AbstractMessageListenerContainer)
				new DirectFieldAccessor(gateway).getPropertyValue("listenerContainer");
		assertEquals(22, new DirectFieldAccessor(container).getPropertyValue("maxConcurrentConsumers"));
		gateway.stop();
	}

	@Test
	public void testGatewayWithMaxMessagesPerTask() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsGatewayWithContainerSettings.xml", this.getClass());
		JmsMessageDrivenEndpoint gateway = (JmsMessageDrivenEndpoint) context.getBean("gatewayWithMaxMessagesPerTask");
		gateway.start();
		AbstractMessageListenerContainer container = (AbstractMessageListenerContainer)
				new DirectFieldAccessor(gateway).getPropertyValue("listenerContainer");
		assertEquals(99, new DirectFieldAccessor(container).getPropertyValue("maxMessagesPerTask"));
		gateway.stop();
	}

	@Test
	public void testGatewayWithReceiveTimeout() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsGatewayWithContainerSettings.xml", this.getClass());
		JmsMessageDrivenEndpoint gateway = (JmsMessageDrivenEndpoint) context.getBean("gatewayWithReceiveTimeout");
		gateway.start();
		AbstractMessageListenerContainer container = (AbstractMessageListenerContainer)
				new DirectFieldAccessor(gateway).getPropertyValue("listenerContainer");
		assertEquals(1111L, new DirectFieldAccessor(container).getPropertyValue("receiveTimeout"));
		gateway.stop();
	}

	@Test
	public void testGatewayWithRecoveryInterval() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsGatewayWithContainerSettings.xml", this.getClass());
		JmsMessageDrivenEndpoint gateway = (JmsMessageDrivenEndpoint) context.getBean("gatewayWithRecoveryInterval");
		gateway.start();
		AbstractMessageListenerContainer container = (AbstractMessageListenerContainer)
				new DirectFieldAccessor(gateway).getPropertyValue("listenerContainer");
		assertEquals(2222L, new DirectFieldAccessor(container).getPropertyValue("recoveryInterval"));
		gateway.stop();
	}

	@Test
	public void testGatewayWithIdleTaskExecutionLimit() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsGatewayWithContainerSettings.xml", this.getClass());
		JmsMessageDrivenEndpoint gateway = (JmsMessageDrivenEndpoint) context.getBean("gatewayWithIdleTaskExecutionLimit");
		gateway.start();
		AbstractMessageListenerContainer container = (AbstractMessageListenerContainer)
				new DirectFieldAccessor(gateway).getPropertyValue("listenerContainer");
		assertEquals(7, new DirectFieldAccessor(container).getPropertyValue("idleTaskExecutionLimit"));
		gateway.stop();
	}

	@Test
	public void testGatewayWithIdleConsumerLimit() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsGatewayWithContainerSettings.xml", this.getClass());
		JmsMessageDrivenEndpoint gateway = (JmsMessageDrivenEndpoint) context.getBean("gatewayWithIdleConsumerLimit");
		gateway.start();
		AbstractMessageListenerContainer container = (AbstractMessageListenerContainer)
				new DirectFieldAccessor(gateway).getPropertyValue("listenerContainer");
		assertEquals(33, new DirectFieldAccessor(container).getPropertyValue("idleConsumerLimit"));
		gateway.stop();
	}

	@Test
	public void testGatewayWithContainerReference() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"inboundGatewayWithContainerReference.xml", this.getClass());
		JmsMessageDrivenEndpoint gateway = (JmsMessageDrivenEndpoint) context.getBean("gatewayWithContainerReference");
		gateway.start();
		AbstractMessageListenerContainer container = (AbstractMessageListenerContainer)
				new DirectFieldAccessor(gateway).getPropertyValue("listenerContainer");
		assertEquals(context.getBean("messageListenerContainer"), container);
		gateway.stop();
	}

	@Test
	public void testGatewayWithMessageSelector() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"inboundGatewayWithMessageSelector.xml", this.getClass());
		JmsMessageDrivenEndpoint gateway = (JmsMessageDrivenEndpoint) context.getBean("gatewayWithMessageSelector");
		gateway.start();
		AbstractMessageListenerContainer container = (AbstractMessageListenerContainer)
				new DirectFieldAccessor(gateway).getPropertyValue("listenerContainer");
		String messageSelector = (String) new DirectFieldAccessor(container).getPropertyValue("messageSelector");
		assertEquals("TestProperty = 'foo'", messageSelector);
		gateway.stop();
	}

	@Test
	public void testGatewayWithReplyQosProperties() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"inboundGatewayWithReplyQos.xml", this.getClass());
		JmsMessageDrivenEndpoint gateway = context.getBean("gatewayWithReplyQos", JmsMessageDrivenEndpoint.class);
		DirectFieldAccessor accessor = new DirectFieldAccessor(
				new DirectFieldAccessor(gateway).getPropertyValue("listener"));
		assertEquals(12345L, accessor.getPropertyValue("replyTimeToLive"));
		assertEquals(7, accessor.getPropertyValue("replyPriority"));
		assertEquals(DeliveryMode.NON_PERSISTENT, accessor.getPropertyValue("replyDeliveryMode"));
		assertEquals(true, accessor.getPropertyValue("explicitQosEnabledForReplies"));
	}

	@Test
	public void replyQosPropertiesDisabledByDefault() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"inboundGatewayDefault.xml", this.getClass());
		JmsMessageDrivenEndpoint gateway = context.getBean("gateway", JmsMessageDrivenEndpoint.class);
		DirectFieldAccessor accessor = new DirectFieldAccessor(
				new DirectFieldAccessor(gateway).getPropertyValue("listener"));
		assertEquals(false, accessor.getPropertyValue("explicitQosEnabledForReplies"));
	}

	@Test
	public void gatewayWithPubSubDomain() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsGatewayWithPubSubDomain.xml", this.getClass());
		JmsMessageDrivenEndpoint endpoint = context.getBean("gateway", JmsMessageDrivenEndpoint.class);
		JmsDestinationAccessor container = (JmsDestinationAccessor) new DirectFieldAccessor(endpoint).getPropertyValue("listenerContainer");
		assertEquals(Boolean.TRUE, container.isPubSubDomain());
	}

	@Test
	public void gatewayWithReplyChannel() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsGatewayWithReplyChannel.xml", this.getClass());
		JmsMessageDrivenEndpoint gateway = (JmsMessageDrivenEndpoint) context.getBean("gateway");
		DirectFieldAccessor accessor = new DirectFieldAccessor(
				new DirectFieldAccessor(gateway).getPropertyValue("listener"));
		Object replyChannel = accessor.getPropertyValue("replyChannel");
		assertEquals(context.getBean("replies"), replyChannel);
	}

}
