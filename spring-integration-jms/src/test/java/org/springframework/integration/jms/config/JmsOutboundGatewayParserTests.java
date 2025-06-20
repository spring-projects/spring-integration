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

package org.springframework.integration.jms.config;

import java.util.Properties;

import jakarta.jms.DeliveryMode;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.expression.Expression;
import org.springframework.integration.core.MessagingTemplate;
import org.springframework.integration.handler.ExpressionEvaluatingMessageProcessor;
import org.springframework.integration.handler.MessageProcessor;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.jms.ActiveMQMultiContextTests;
import org.springframework.integration.jms.JmsOutboundGateway;
import org.springframework.integration.jms.StubMessageConverter;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Jonas Partner
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Gary Russell
 */
public class JmsOutboundGatewayParserTests extends ActiveMQMultiContextTests {

	private static volatile int adviceCalled;

	@Test
	public void testWithDeliveryPersistentAttribute() {
		try (var context = new ClassPathXmlApplicationContext(
				"jmsOutboundGatewayWithDeliveryPersistent.xml", getClass())) {

			var endpoint = context.getBean("jmsGateway");
			var gateway = TestUtils.getPropertyValue(endpoint, "handler");
			assertThat(TestUtils.getPropertyValue(gateway, "deliveryMode")).isEqualTo(DeliveryMode.PERSISTENT);
			assertThat(TestUtils.getPropertyValue(gateway, "async", Boolean.class)).isTrue();
			var container = TestUtils.getPropertyValue(gateway, "replyContainer",
					DefaultMessageListenerContainer.class);
			assertThat(TestUtils.getPropertyValue(container, "concurrentConsumers")).isEqualTo(4);
			assertThat(TestUtils.getPropertyValue(container, "maxConcurrentConsumers")).isEqualTo(5);
			assertThat(TestUtils.getPropertyValue(container, "maxMessagesPerTask")).isEqualTo(10);
			assertThat(TestUtils.getPropertyValue(container, "receiveTimeout")).isEqualTo(2000L);
			assertThat(TestUtils.getPropertyValue(container, "backOff.interval")).isEqualTo(10000L);
			assertThat(TestUtils.getPropertyValue(container, "idleConsumerLimit")).isEqualTo(7);
			assertThat(TestUtils.getPropertyValue(container, "idleTaskExecutionLimit")).isEqualTo(2);
			assertThat(TestUtils.getPropertyValue(container, "cacheLevel")).isEqualTo(3);
			assertThat(container.isSessionTransacted()).isTrue();
			assertThat(TestUtils.getPropertyValue(container, "taskExecutor")).isSameAs(context.getBean("exec"));
			assertThat(TestUtils.getPropertyValue(gateway, "idleReplyContainerTimeout")).isEqualTo(1234000L);
		}
	}

	@Test
	public void testAdvised() {
		try (var context = new ClassPathXmlApplicationContext(
				"jmsOutboundGatewayWithDeliveryPersistent.xml", getClass())) {

			var endpoint = context.getBean("advised");
			JmsOutboundGateway gateway = TestUtils.getPropertyValue(endpoint, "handler", JmsOutboundGateway.class);
			assertThat(TestUtils.getPropertyValue(gateway, "async", Boolean.class)).isFalse();
			gateway.handleMessage(new GenericMessage<>("foo"));
			assertThat(adviceCalled).isEqualTo(1);
			assertThat(TestUtils.getPropertyValue(gateway, "replyContainer.sessionAcknowledgeMode")).isEqualTo(3);
		}
	}

	@Test
	public void testDefault() {
		try (var context = new ClassPathXmlApplicationContext("jmsOutboundGatewayWithConverter.xml", getClass())) {
			var endpoint = context.getBean("jmsGateway");
			assertThat(TestUtils.getPropertyValue(endpoint, "handler.messageConverter"))
					.isInstanceOf(StubMessageConverter.class);
		}
	}

	@Test
	public void gatewayWithOrder() {
		try (var context = new ClassPathXmlApplicationContext("jmsOutboundGatewayWithOrder.xml", getClass())) {
			var endpoint = context.getBean("jmsGateway");
			assertThat(TestUtils.getPropertyValue(endpoint, "handler.requiresReply")).isEqualTo(Boolean.TRUE);
			assertThat(TestUtils.getPropertyValue(endpoint, "handler.order")).isEqualTo(99);
		}
	}

	@Test
	public void gatewayWithDest() {
		try (var context = new ClassPathXmlApplicationContext("jmsOutboundGatewayReplyDestOptions.xml", getClass())) {
			var endpoint = context.getBean("jmsGatewayDest");
			assertThat(TestUtils.getPropertyValue(endpoint, "handler.replyDestination"))
					.isSameAs(context.getBean("replyQueue"));
		}
	}

	@Test
	public void gatewayWithDestName() {
		try (var context = new ClassPathXmlApplicationContext("jmsOutboundGatewayReplyDestOptions.xml", getClass())) {
			var endpoint = context.getBean("jmsGatewayDestName");
			assertThat(TestUtils.getPropertyValue(endpoint, "handler.replyDestinationName")).isEqualTo("replyQueueName");
		}
	}

	@Test
	public void gatewayWithDestBeanRefExpression() {
		try (var context = new ClassPathXmlApplicationContext("jmsOutboundGatewayReplyDestOptions.xml", getClass())) {
			var endpoint = context.getBean("jmsGatewayDestExpressionBeanRef");
			MessageProcessor<?> processor =
					TestUtils.getPropertyValue(endpoint, "handler.replyDestinationExpressionProcessor",
							ExpressionEvaluatingMessageProcessor.class);
			var expression = TestUtils.getPropertyValue(endpoint,
					"handler.replyDestinationExpressionProcessor.expression", Expression.class);
			assertThat(expression.getExpressionString()).isEqualTo("@replyQueue");
			assertThat(processor.processMessage(null)).isSameAs(context.getBean("replyQueue"));
		}
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
		var context = new ClassPathXmlApplicationContext("gatewayMaintainsReplyChannel.xml", getClass());
		SampleGateway gateway = context.getBean("gateway", SampleGateway.class);
		SubscribableChannel jmsInput = context.getBean("jmsInput", SubscribableChannel.class);
		MessageHandler handler =
				message -> {
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
		try (var context = new ClassPathXmlApplicationContext("jmsOutboundGatewayWithPubSubSettings.xml", getClass())) {
			var endpoint = context.getBean("defaultGateway");
			assertThat(TestUtils.getPropertyValue(endpoint, "handler.requestPubSubDomain", Boolean.class)).isFalse();
			assertThat(TestUtils.getPropertyValue(endpoint, "handler.replyPubSubDomain", Boolean.class)).isFalse();
		}
	}

	@Test
	public void gatewayWithExplicitPubSubDomainTrue() {
		try (var context = new ClassPathXmlApplicationContext("jmsOutboundGatewayWithPubSubSettings.xml", getClass())) {
			var endpoint = context.getBean("pubSubDomainGateway");
			assertThat(TestUtils.getPropertyValue(endpoint, "handler.requestPubSubDomain", Boolean.class)).isTrue();
			assertThat(TestUtils.getPropertyValue(endpoint, "handler.replyPubSubDomain", Boolean.class)).isTrue();
		}
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
