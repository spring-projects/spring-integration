/*
 * Copyright 2002-2021 the original author or authors.
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

package org.springframework.integration.jms.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Properties;

import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;

import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.NotReadablePropertyException;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.jms.ActiveMQMultiContextTests;
import org.springframework.integration.jms.JmsMessageDrivenEndpoint;
import org.springframework.integration.support.SmartLifecycleRoleController;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.jms.connection.JmsTransactionManager;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.support.destination.JmsDestinationAccessor;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.util.MultiValueMap;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 */
public class JmsInboundGatewayParserTests extends ActiveMQMultiContextTests {

	@Test
	public void testGatewayWithConnectionFactoryAndDestination() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsGatewayWithConnectionFactoryAndDestination.xml", this.getClass());
		PollableChannel channel = (PollableChannel) context.getBean("requestChannel");
		JmsMessageDrivenEndpoint gateway = (JmsMessageDrivenEndpoint) context.getBean("jmsGateway");
		assertThat(gateway.getClass()).isEqualTo(JmsMessageDrivenEndpoint.class);
		context.start();
		Message<?> message = channel.receive(10000);
		MessageHistory history = MessageHistory.read(message);
		assertThat(history).isNotNull();
		Properties componentHistoryRecord = TestUtils.locateComponentInHistory(history, "jmsGateway", 0);
		assertThat(componentHistoryRecord).isNotNull();
		assertThat(componentHistoryRecord.get("type")).isEqualTo("jms:inbound-gateway");
		assertThat(message).as("message should not be null").isNotNull();
		assertThat(message.getPayload()).isEqualTo("message-driven-test");
		context.close();
	}

	@Test
	public void testGatewayWithConnectionFactoryAndDestinationName() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsGatewayWithConnectionFactoryAndDestinationName.xml", this.getClass());
		PollableChannel channel = (PollableChannel) context.getBean("requestChannel");
		JmsMessageDrivenEndpoint gateway = (JmsMessageDrivenEndpoint) context.getBean("jmsGateway");
		assertThat(gateway.getClass()).isEqualTo(JmsMessageDrivenEndpoint.class);
		context.start();
		Message<?> message = channel.receive(10000);
		assertThat(message).as("message should not be null").isNotNull();
		assertThat(message.getPayload()).isEqualTo("message-driven-test");
		context.close();
	}

	@Test
	public void testGatewayWithMessageConverter() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsGatewayWithMessageConverter.xml", this.getClass());
		PollableChannel channel = (PollableChannel) context.getBean("requestChannel");
		JmsMessageDrivenEndpoint gateway = (JmsMessageDrivenEndpoint) context.getBean("jmsGateway");
		assertThat(gateway.getClass()).isEqualTo(JmsMessageDrivenEndpoint.class);
		context.start();
		Message<?> message = channel.receive(10000);
		assertThat(message).as("message should not be null").isNotNull();
		assertThat(message.getPayload()).isEqualTo("converted-test-message");
		context.close();
	}

	@Test
	public void testGatewayWithExtractPayloadAttributes() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsGatewaysWithExtractPayloadAttributes.xml", this.getClass());

		JmsMessageDrivenEndpoint gateway = (JmsMessageDrivenEndpoint) context.getBean("defaultGateway");
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		accessor = new DirectFieldAccessor(accessor.getPropertyValue("listener"));
		assertThat(accessor.getPropertyValue("extractReplyPayload")).isEqualTo(Boolean.TRUE);

		gateway = (JmsMessageDrivenEndpoint) context.getBean("extractReplyPayloadTrue");
		accessor = new DirectFieldAccessor(gateway);
		accessor = new DirectFieldAccessor(accessor.getPropertyValue("listener"));
		assertThat(accessor.getPropertyValue("extractReplyPayload")).isEqualTo(Boolean.TRUE);

		gateway = (JmsMessageDrivenEndpoint) context.getBean("extractReplyPayloadFalse");
		accessor = new DirectFieldAccessor(gateway);
		accessor = new DirectFieldAccessor(accessor.getPropertyValue("listener"));
		assertThat(accessor.getPropertyValue("extractReplyPayload")).isEqualTo(Boolean.FALSE);

		gateway = (JmsMessageDrivenEndpoint) context.getBean("extractRequestPayloadTrue");
		accessor = new DirectFieldAccessor(gateway);
		accessor = new DirectFieldAccessor(accessor.getPropertyValue("listener"));
		assertThat(accessor.getPropertyValue("extractRequestPayload")).isEqualTo(Boolean.TRUE);

		gateway = (JmsMessageDrivenEndpoint) context.getBean("extractRequestPayloadFalse");
		accessor = new DirectFieldAccessor(gateway);
		accessor = new DirectFieldAccessor(accessor.getPropertyValue("listener"));
		assertThat(accessor.getPropertyValue("extractRequestPayload")).isEqualTo(Boolean.FALSE);
		context.close();
	}

	@Test(expected = BeanDefinitionStoreException.class)
	public void testGatewayWithConnectionFactoryOnly() {
		try {
			new ClassPathXmlApplicationContext("jmsGatewayWithConnectionFactoryOnly.xml", this.getClass()).close();
		}
		catch (BeanDefinitionStoreException e) {
			assertThat(e.getMessage().contains("request-destination")).isTrue();
			assertThat(e.getMessage().contains("request-destination-name")).isTrue();
			throw e;
		}
	}

	@Test(expected = BeanDefinitionStoreException.class)
	public void testGatewayWithEmptyConnectionFactory() {
		try {
			new ClassPathXmlApplicationContext("jmsGatewayWithEmptyConnectionFactory.xml", this.getClass()).close();
		}
		catch (BeanDefinitionStoreException e) {
			assertThat(e.getMessage().contains("connection-factory")).isTrue();
			throw e;
		}
	}

	@Test
	public void testGatewayWithDefaultConnectionFactory() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsGatewayWithDefaultConnectionFactory.xml", this.getClass());
		PollableChannel channel = (PollableChannel) context.getBean("requestChannel");
		JmsMessageDrivenEndpoint gateway = (JmsMessageDrivenEndpoint) context.getBean("jmsGateway");
		assertThat(gateway.getClass()).isEqualTo(JmsMessageDrivenEndpoint.class);
		context.start();
		Message<?> message = channel.receive(10000);
		assertThat(message).as("message should not be null").isNotNull();
		assertThat(message.getPayload()).isEqualTo("message-driven-test");
		context.close();
	}

	@Test
	public void testTransactionManager() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsGatewayTransactionManagerTests.xml", this.getClass());
		JmsMessageDrivenEndpoint gateway = (JmsMessageDrivenEndpoint) context.getBean("gatewayWithoutTransactionManager");
		DirectFieldAccessor accessor = new DirectFieldAccessor(gateway);
		accessor = new DirectFieldAccessor(accessor.getPropertyValue("listenerContainer"));
		assertThat(accessor.getPropertyValue("transactionManager")).isNull();

		gateway = (JmsMessageDrivenEndpoint) context.getBean("gatewayWithTransactionManager");
		accessor = new DirectFieldAccessor(gateway);
		accessor = new DirectFieldAccessor(accessor.getPropertyValue("listenerContainer"));
		Object txManager = accessor.getPropertyValue("transactionManager");
		assertThat(txManager.getClass()).isEqualTo(JmsTransactionManager.class);
		assertThat(txManager).isEqualTo(context.getBean("txManager"));
		assertThat(((JmsTransactionManager) txManager).getConnectionFactory())
				.isEqualTo(context.getBean("testConnectionFactory"));
		context.close();
	}

	@Test
	public void testGatewayWithContainerSettings() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsGatewayWithContainerSettings.xml", this.getClass());

		JmsMessageDrivenEndpoint gateway = (JmsMessageDrivenEndpoint) context.getBean("gatewayWithConcurrentConsumers");
		AbstractMessageListenerContainer container = (AbstractMessageListenerContainer)
				new DirectFieldAccessor(gateway).getPropertyValue("listenerContainer");
		assertThat(new DirectFieldAccessor(container).getPropertyValue("concurrentConsumers")).isEqualTo(3);

		gateway = (JmsMessageDrivenEndpoint) context.getBean("gatewayWithMaxConcurrentConsumers");
		container = (AbstractMessageListenerContainer)
				new DirectFieldAccessor(gateway).getPropertyValue("listenerContainer");
		assertThat(new DirectFieldAccessor(container).getPropertyValue("maxConcurrentConsumers")).isEqualTo(22);

		gateway = (JmsMessageDrivenEndpoint) context.getBean("gatewayWithMaxMessagesPerTask");
		container = (AbstractMessageListenerContainer)
				new DirectFieldAccessor(gateway).getPropertyValue("listenerContainer");
		assertThat(new DirectFieldAccessor(container).getPropertyValue("maxMessagesPerTask")).isEqualTo(99);

		gateway = (JmsMessageDrivenEndpoint) context.getBean("gatewayWithReceiveTimeout");
		container = (AbstractMessageListenerContainer)
				new DirectFieldAccessor(gateway).getPropertyValue("listenerContainer");
		assertThat(new DirectFieldAccessor(container).getPropertyValue("receiveTimeout")).isEqualTo(1111L);

		gateway = (JmsMessageDrivenEndpoint) context.getBean("gatewayWithRecoveryInterval");
		container = (AbstractMessageListenerContainer)
				new DirectFieldAccessor(gateway).getPropertyValue("listenerContainer");
		Object recoveryInterval;
		try {
			recoveryInterval = TestUtils.getPropertyValue(container, "recoveryInterval");
		}
		catch (NotReadablePropertyException e) {
			recoveryInterval = TestUtils.getPropertyValue(container, "backOff.interval");
		}
		assertThat(recoveryInterval).isEqualTo(2222L);

		gateway = (JmsMessageDrivenEndpoint) context.getBean("gatewayWithIdleTaskExecutionLimit");
		container = (AbstractMessageListenerContainer)
				new DirectFieldAccessor(gateway).getPropertyValue("listenerContainer");
		assertThat(new DirectFieldAccessor(container).getPropertyValue("idleTaskExecutionLimit")).isEqualTo(7);

		gateway = (JmsMessageDrivenEndpoint) context.getBean("gatewayWithIdleConsumerLimit");
		container = (AbstractMessageListenerContainer)
				new DirectFieldAccessor(gateway).getPropertyValue("listenerContainer");
		assertThat(new DirectFieldAccessor(container).getPropertyValue("idleConsumerLimit")).isEqualTo(33);

		gateway = (JmsMessageDrivenEndpoint) context.getBean("gatewayWithMessageSelector");
		container = (AbstractMessageListenerContainer)
				new DirectFieldAccessor(gateway).getPropertyValue("listenerContainer");
		String messageSelector = (String) new DirectFieldAccessor(container).getPropertyValue("messageSelector");
		assertThat(messageSelector).isEqualTo("TestProperty = 'foo'");

		context.close();
	}

	@Test
	public void testGatewayWithContainerReference() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"inboundGatewayWithContainerReference.xml", this.getClass());
		JmsMessageDrivenEndpoint gateway =
				context.getBean("gatewayWithContainerReference", JmsMessageDrivenEndpoint.class);
		AbstractMessageListenerContainer messageListenerContainer = context.getBean("messageListenerContainer",
				AbstractMessageListenerContainer.class);
		assertThat(gateway.isRunning()).isFalse();
		assertThat(messageListenerContainer.isRunning()).isFalse();
		SmartLifecycleRoleController roleController = context.getBean(SmartLifecycleRoleController.class);
		@SuppressWarnings("unchecked")
		MultiValueMap<String, SmartLifecycle> lifecycles =
				TestUtils.getPropertyValue(roleController, "lifecycles", MultiValueMap.class);
		assertThat(lifecycles.containsKey("foo")).isTrue();
		assertThat(lifecycles.getFirst("foo")).isSameAs(gateway);
		gateway.start();
		AbstractMessageListenerContainer container = (AbstractMessageListenerContainer)
				new DirectFieldAccessor(gateway).getPropertyValue("listenerContainer");
		assertThat(container).isEqualTo(messageListenerContainer);
		assertThat(gateway.isRunning()).isTrue();
		assertThat(messageListenerContainer.isRunning()).isTrue();
		gateway.stop();
		context.close();
	}

	@Test
	public void testGatewayWithReplyQosProperties() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"inboundGatewayWithReplyQos.xml", this.getClass());
		JmsMessageDrivenEndpoint gateway = context.getBean("gatewayWithReplyQos", JmsMessageDrivenEndpoint.class);
		DirectFieldAccessor accessor = new DirectFieldAccessor(
				new DirectFieldAccessor(gateway).getPropertyValue("listener"));
		assertThat(accessor.getPropertyValue("replyTimeToLive")).isEqualTo(12345L);
		assertThat(accessor.getPropertyValue("replyPriority")).isEqualTo(7);
		assertThat(accessor.getPropertyValue("replyDeliveryMode")).isEqualTo(DeliveryMode.NON_PERSISTENT);
		assertThat(accessor.getPropertyValue("explicitQosEnabledForReplies")).isEqualTo(true);
		context.close();
	}

	@Test
	public void replyQosPropertiesDisabledByDefault() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"inboundGatewayDefault.xml", this.getClass());
		JmsMessageDrivenEndpoint gateway = context.getBean("gateway", JmsMessageDrivenEndpoint.class);
		DirectFieldAccessor accessor = new DirectFieldAccessor(
				new DirectFieldAccessor(gateway).getPropertyValue("listener"));
		assertThat(accessor.getPropertyValue("explicitQosEnabledForReplies")).isEqualTo(false);
		context.close();
	}

	@Test
	public void gatewayWithPubSubDomain() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsGatewayWithPubSubDomain.xml", this.getClass());
		JmsMessageDrivenEndpoint endpoint = context.getBean("gateway", JmsMessageDrivenEndpoint.class);
		JmsDestinationAccessor container =
				(JmsDestinationAccessor) new DirectFieldAccessor(endpoint).getPropertyValue("listenerContainer");
		assertThat(container.isPubSubDomain()).isEqualTo(Boolean.TRUE);
		context.close();
	}

	@Test
	public void gatewayWithDurableSubscription() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"inboundGatewayWithDurableSubscription.xml", this.getClass());
		JmsMessageDrivenEndpoint endpoint = context.getBean("gateway", JmsMessageDrivenEndpoint.class);
		DefaultMessageListenerContainer container =
				(DefaultMessageListenerContainer) new DirectFieldAccessor(endpoint)
						.getPropertyValue("listenerContainer");
		assertThat(container.isPubSubDomain()).isEqualTo(Boolean.TRUE);
		assertThat(container.isSubscriptionDurable()).isEqualTo(Boolean.TRUE);
		assertThat(container.getDurableSubscriptionName()).isEqualTo("testDurableSubscriptionName");
		assertThat(container.getClientId()).isEqualTo("testClientId");
		assertThat(container.isSubscriptionShared()).isTrue();
		context.close();
	}

	@Test
	public void gatewayWithReplyChannel() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsGatewayWithReplyChannel.xml", this.getClass());
		JmsTemplate template = new JmsTemplate(context.getBean(ConnectionFactory.class));
		template.convertAndSend("testDestination", "Hello");

		JmsMessageDrivenEndpoint gateway = (JmsMessageDrivenEndpoint) context.getBean("gateway");
		Object replyChannel = TestUtils.getPropertyValue(gateway, "listener.gatewayDelegate.replyChannel");
		assertThat(replyChannel).isEqualTo(context.getBean("replies"));

		assertThat(template.receive("testReplyDestination")).isNotNull();

		context.close();
	}

}
