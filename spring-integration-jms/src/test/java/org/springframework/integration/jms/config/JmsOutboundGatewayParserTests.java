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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.Properties;

import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.Queue;
import javax.jms.Session;

import org.junit.jupiter.api.Test;

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
	public void testWithDeliveryPersistentAttribute() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsOutboundGatewayWithDeliveryPersistent.xml", this.getClass());
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context.getBean("jmsGateway");
		DirectFieldAccessor accessor = new DirectFieldAccessor(endpoint);
		JmsOutboundGateway gateway = (JmsOutboundGateway) accessor.getPropertyValue("handler");
		accessor = new DirectFieldAccessor(gateway);
		int deliveryMode = (Integer) accessor.getPropertyValue("deliveryMode");
		assertThat(deliveryMode).isEqualTo(DeliveryMode.PERSISTENT);
		assertThat(TestUtils.getPropertyValue(gateway, "async", Boolean.class)).isTrue();
		DefaultMessageListenerContainer container = TestUtils.getPropertyValue(gateway, "replyContainer",
				DefaultMessageListenerContainer.class);
		assertThat(TestUtils.getPropertyValue(container, "concurrentConsumers")).isEqualTo(4);
		assertThat(TestUtils.getPropertyValue(container, "maxConcurrentConsumers")).isEqualTo(5);
		assertThat(TestUtils.getPropertyValue(container, "maxMessagesPerTask")).isEqualTo(10);
		assertThat(TestUtils.getPropertyValue(container, "receiveTimeout")).isEqualTo(2000L);
		Object recoveryInterval;
		try {
			recoveryInterval = TestUtils.getPropertyValue(container, "recoveryInterval");
		}
		catch (NotReadablePropertyException e) {
			recoveryInterval = TestUtils.getPropertyValue(container, "backOff.interval");
		}
		assertThat(recoveryInterval).isEqualTo(10000L);

		assertThat(TestUtils.getPropertyValue(container, "idleConsumerLimit")).isEqualTo(7);
		assertThat(TestUtils.getPropertyValue(container, "idleTaskExecutionLimit")).isEqualTo(2);
		assertThat(TestUtils.getPropertyValue(container, "cacheLevel")).isEqualTo(3);
		assertThat(container.isSessionTransacted()).isTrue();
		assertThat(TestUtils.getPropertyValue(container, "taskExecutor")).isSameAs(context.getBean("exec"));
		assertThat(TestUtils.getPropertyValue(gateway, "idleReplyContainerTimeout")).isEqualTo(1234000L);
		context.close();
	}

	@Test
	public void testAdvised() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsOutboundGatewayWithDeliveryPersistent.xml", this.getClass());
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context.getBean("advised");
		JmsOutboundGateway gateway = TestUtils.getPropertyValue(endpoint, "handler", JmsOutboundGateway.class);
		assertThat(TestUtils.getPropertyValue(gateway, "async", Boolean.class)).isFalse();
		gateway.handleMessage(new GenericMessage<>("foo"));
		assertThat(adviceCalled).isEqualTo(1);
		assertThat(TestUtils.getPropertyValue(gateway, "replyContainer.sessionAcknowledgeMode")).isEqualTo(3);
		context.close();
	}

	@Test
	public void testDefault() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsOutboundGatewayWithConverter.xml", this.getClass());
		PollingConsumer endpoint = (PollingConsumer) context.getBean("jmsGateway");
		DirectFieldAccessor accessor = new DirectFieldAccessor(endpoint);
		JmsOutboundGateway gateway = (JmsOutboundGateway) accessor.getPropertyValue("handler");
		accessor = new DirectFieldAccessor(gateway);
		MessageConverter converter = (MessageConverter) accessor.getPropertyValue("messageConverter");
		assertThat(converter instanceof StubMessageConverter).as("Wrong message converter").isTrue();
		context.close();
	}

	@Test
	public void gatewayWithOrder() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsOutboundGatewayWithOrder.xml", this.getClass());
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context.getBean("jmsGateway");
		DirectFieldAccessor accessor = new DirectFieldAccessor(
				new DirectFieldAccessor(endpoint).getPropertyValue("handler"));
		Object order = accessor.getPropertyValue("order");
		assertThat(order).isEqualTo(99);
		assertThat(accessor.getPropertyValue("requiresReply")).isEqualTo(Boolean.TRUE);
		context.close();
	}

	@Test
	public void gatewayWithDest() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsOutboundGatewayReplyDestOptions.xml", this.getClass());
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context.getBean("jmsGatewayDest");
		DirectFieldAccessor accessor = new DirectFieldAccessor(endpoint);
		JmsOutboundGateway gateway = (JmsOutboundGateway) accessor.getPropertyValue("handler");
		accessor = new DirectFieldAccessor(gateway);
		assertThat(accessor.getPropertyValue("replyDestination")).isSameAs(context.getBean("replyQueue"));
		context.close();
	}

	@Test
	public void gatewayWithDestName() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsOutboundGatewayReplyDestOptions.xml", this.getClass());
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context.getBean("jmsGatewayDestName");
		DirectFieldAccessor accessor = new DirectFieldAccessor(endpoint);
		JmsOutboundGateway gateway = (JmsOutboundGateway) accessor.getPropertyValue("handler");
		accessor = new DirectFieldAccessor(gateway);
		assertThat(accessor.getPropertyValue("replyDestinationName")).isEqualTo("replyQueueName");
		context.close();
	}

	@Test
	public void gatewayWithDestExpression() throws Exception {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsOutboundGatewayReplyDestOptions.xml", this.getClass());
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context.getBean("jmsGatewayDestExpression");
		DirectFieldAccessor accessor = new DirectFieldAccessor(endpoint);
		JmsOutboundGateway gateway = (JmsOutboundGateway) accessor.getPropertyValue("handler");
		ExpressionEvaluatingMessageProcessor<?> processor =
				TestUtils.getPropertyValue(gateway, "replyDestinationExpressionProcessor",
						ExpressionEvaluatingMessageProcessor.class);
		Expression expression = TestUtils.getPropertyValue(gateway, "replyDestinationExpressionProcessor.expression",
				Expression.class);
		assertThat(expression.getExpressionString()).isEqualTo("payload");
		Message<?> message = MessageBuilder.withPayload("foo").build();
		assertThat(processor.processMessage(message)).isEqualTo("foo");

		Method method =
				JmsOutboundGateway.class.getDeclaredMethod("determineReplyDestination", Message.class, Session.class);
		method.setAccessible(true);

		Session session = mock(Session.class);
		Queue queue = mock(Queue.class);
		when(session.createQueue("foo")).thenReturn(queue);
		Destination replyQ = (Destination) method.invoke(gateway, message, session);
		assertThat(replyQ).isSameAs(queue);
		context.close();
	}

	@Test
	public void gatewayWithDestBeanRefExpression() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsOutboundGatewayReplyDestOptions.xml", this.getClass());
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context.getBean("jmsGatewayDestExpressionBeanRef");
		DirectFieldAccessor accessor = new DirectFieldAccessor(endpoint);
		JmsOutboundGateway gateway = (JmsOutboundGateway) accessor.getPropertyValue("handler");
		ExpressionEvaluatingMessageProcessor<?> processor =
				TestUtils.getPropertyValue(gateway, "replyDestinationExpressionProcessor",
						ExpressionEvaluatingMessageProcessor.class);
		Expression expression = TestUtils.getPropertyValue(gateway, "replyDestinationExpressionProcessor.expression",
				Expression.class);
		assertThat(expression.getExpressionString()).isEqualTo("@replyQueue");
		assertThat(processor.processMessage(null)).isSameAs(context.getBean("replyQueue"));
		context.close();
	}

	@Test
	public void gatewayWithDestAndDestExpression() {
		assertThatExceptionOfType(BeanDefinitionParsingException.class)
				.isThrownBy(() ->
						new ClassPathXmlApplicationContext("jmsOutboundGatewayReplyDestOptions-fail.xml", getClass()))
				.withMessageStartingWith("Configuration problem: Only one of the " +
						"'replyQueue', 'reply-destination-name', or 'reply-destination-expression' " +
						"attributes is allowed.");
	}

	@Test
	public void gatewayMaintainsReplyChannelAndInboundHistory() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"gatewayMaintainsReplyChannel.xml", this.getClass());
		SampleGateway gateway = context.getBean("gateway", SampleGateway.class);
		SubscribableChannel jmsInput = context.getBean("jmsInput", SubscribableChannel.class);
		MessageHandler handler = message -> {
			MessageHistory history = MessageHistory.read(message);
			assertThat(history).isNotNull();
			Properties componentHistoryRecord = TestUtils.locateComponentInHistory(history, "inboundGateway", 0);
			assertThat(componentHistoryRecord).isNotNull();
			assertThat(componentHistoryRecord.get("type")).isEqualTo("jms:inbound-gateway");
			MessagingTemplate messagingTemplate = new MessagingTemplate();
			messagingTemplate.setDefaultDestination((MessageChannel) message.getHeaders().getReplyChannel());
			messagingTemplate.send(message);
		};
		jmsInput.subscribe(handler);
		String result = gateway.echo("hello");
		assertThat(result).isEqualTo("hello");
		JmsOutboundGateway gw1 = context.getBean("chain1$child.gateway.handler", JmsOutboundGateway.class);
		MessageChannel out = TestUtils.getPropertyValue(gw1, "outputChannel", MessageChannel.class);
		assertThat(out.getClass().getSimpleName()).isEqualTo("ReplyForwardingMessageChannel");
		JmsOutboundGateway gw2 = context.getBean("chain2$child.gateway.handler", JmsOutboundGateway.class);
		out = TestUtils.getPropertyValue(gw2, "outputChannel", MessageChannel.class);
		assertThat(out.getClass().getName()).contains("MessageHandlerChain$");
		context.close();
	}

	@Test
	public void gatewayWithDefaultPubSubDomain() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsOutboundGatewayWithPubSubSettings.xml", this.getClass());
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context.getBean("defaultGateway");
		DirectFieldAccessor accessor = new DirectFieldAccessor(
				new DirectFieldAccessor(endpoint).getPropertyValue("handler"));
		assertThat((Boolean) accessor.getPropertyValue("requestPubSubDomain")).isFalse();
		assertThat((Boolean) accessor.getPropertyValue("replyPubSubDomain")).isFalse();
		context.close();
	}

	@Test
	public void gatewayWithExplicitPubSubDomainTrue() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsOutboundGatewayWithPubSubSettings.xml", this.getClass());
		EventDrivenConsumer endpoint = (EventDrivenConsumer) context.getBean("pubSubDomainGateway");
		DirectFieldAccessor accessor = new DirectFieldAccessor(
				new DirectFieldAccessor(endpoint).getPropertyValue("handler"));
		assertThat((Boolean) accessor.getPropertyValue("requestPubSubDomain")).isTrue();
		assertThat((Boolean) accessor.getPropertyValue("replyPubSubDomain")).isTrue();
		context.close();
	}


	public interface SampleGateway {

		String echo(String value);

	}

	public static class SampleService {

		public String echo(String value) {
			return value.toUpperCase();
		}

	}

	public static class FooAdvice extends AbstractRequestHandlerAdvice {

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) {
			adviceCalled++;
			return null;
		}

	}

}
