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

package org.springframework.integration.amqp.config;

import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.AsyncRabbitTemplate;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.amqp.outbound.AmqpOutboundEndpoint;
import org.springframework.integration.amqp.outbound.AsyncAmqpOutboundGateway;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.context.Orderable;
import org.springframework.integration.handler.advice.AbstractRequestHandlerAdvice;
import org.springframework.integration.support.ErrorMessageStrategy;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.isNull;

/**
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @author Gunnar Hillert
 * @author Glenn Renfro
 *
 * @since 2.1
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class AmqpOutboundGatewayParserTests {

	private static volatile int adviceCalled;

	@Autowired
	private ApplicationContext context;

	@Test
	public void testGatewayConfig() {
		Object edc = this.context.getBean("rabbitGateway");
		assertThat(TestUtils.<Boolean>getPropertyValue(edc, "autoStartup")).isFalse();
		AmqpOutboundEndpoint gateway = TestUtils.getPropertyValue(edc, "handler");
		assertThat(gateway.getComponentType()).isEqualTo("amqp:outbound-gateway");
		assertThat(TestUtils.<Boolean>getPropertyValue(gateway, "requiresReply")).isTrue();
		checkGWProps(this.context, gateway);

		AsyncAmqpOutboundGateway async = this.context.getBean("asyncGateway.handler", AsyncAmqpOutboundGateway.class);
		assertThat(async.getComponentType()).isEqualTo("amqp:outbound-async-gateway");
		checkGWProps(this.context, async);
		assertThat(TestUtils.<AsyncRabbitTemplate>getPropertyValue(async, "template"))
				.isSameAs(this.context.getBean("asyncTemplate"));
		assertThat(TestUtils.<ErrorMessageStrategy>getPropertyValue(gateway, "errorMessageStrategy"))
				.isSameAs(this.context.getBean("ems"));
	}

	protected void checkGWProps(ApplicationContext context, Orderable gateway) {
		assertThat(gateway.getOrder()).isEqualTo(5);
		assertThat(TestUtils.<MessageChannel>getPropertyValue(gateway, "outputChannel"))
				.isEqualTo(context.getBean("fromRabbit"));
		MessageChannel returnChannel = context.getBean("returnChannel", MessageChannel.class);
		assertThat(TestUtils.<MessageChannel>getPropertyValue(gateway, "returnChannel")).isSameAs(returnChannel);

		Long sendTimeout = TestUtils.getPropertyValue(gateway, "messagingTemplate.sendTimeout");

		assertThat(sendTimeout).isEqualTo(Long.valueOf(777));
		assertThat(TestUtils.<Boolean>getPropertyValue(gateway, "lazyConnect")).isTrue();
		assertThat(TestUtils.<org.springframework.expression.Expression>getPropertyValue(gateway, "delayExpression")
				.getExpressionString()).isEqualTo("42");
	}

	@Test
	public void withHeaderMapperCustomRequestResponse() {
		Object eventDrivenConsumer = this.context.getBean("withHeaderMapperCustomRequestResponse");

		AmqpOutboundEndpoint endpoint = TestUtils.getPropertyValue(eventDrivenConsumer, "handler");
		assertThat(TestUtils.<Object>getPropertyValue(endpoint, "defaultDeliveryMode")).isNotNull();
		assertThat(TestUtils.<Boolean>getPropertyValue(endpoint, "lazyConnect")).isFalse();

		assertThat(TestUtils.<Boolean>getPropertyValue(endpoint, "requiresReply")).isFalse();
		assertThat(TestUtils.<Boolean>getPropertyValue(endpoint, "headersMappedLast")).isTrue();

		Field amqpTemplateField = ReflectionUtils.findField(AmqpOutboundEndpoint.class, "rabbitTemplate");
		amqpTemplateField.setAccessible(true);
		RabbitTemplate amqpTemplate = TestUtils.getPropertyValue(endpoint, "rabbitTemplate");
		amqpTemplate = Mockito.spy(amqpTemplate);
		final AtomicBoolean shouldBePersistent = new AtomicBoolean();

		Mockito.doAnswer(invocation -> {
					Object[] args = invocation.getArguments();
					org.springframework.amqp.core.Message amqpRequestMessage = (org.springframework.amqp.core.Message) args[2];
					MessageProperties properties = amqpRequestMessage.getMessageProperties();
					assertThat(properties.getHeaders().get("foo")).isEqualTo("foo");
					assertThat(properties.getDeliveryMode()).isEqualTo(shouldBePersistent.get() ?
							MessageDeliveryMode.PERSISTENT
							: MessageDeliveryMode.NON_PERSISTENT);
					// mock reply AMQP message
					MessageProperties amqpProperties = new MessageProperties();
					amqpProperties.setAppId("test.appId");
					amqpProperties.setHeader("foobar", "foobar");
					amqpProperties.setHeader("bar", "bar");
					return new org.springframework.amqp.core.Message("hello".getBytes(), amqpProperties);
				})
				.when(amqpTemplate).sendAndReceive(Mockito.any(String.class), Mockito.any(String.class),
						Mockito.any(org.springframework.amqp.core.Message.class), isNull());
		ReflectionUtils.setField(amqpTemplateField, endpoint, amqpTemplate);

		MessageChannel requestChannel = this.context.getBean("toRabbit1", MessageChannel.class);
		Message<?> message = MessageBuilder.withPayload("hello").setHeader("foo", "foo").build();
		requestChannel.send(message);

		Mockito.verify(amqpTemplate, Mockito.times(1)).sendAndReceive(Mockito.any(String.class),
				Mockito.any(String.class), Mockito.any(org.springframework.amqp.core.Message.class),
				isNull());

		// verify reply
		QueueChannel queueChannel = this.context.getBean("fromRabbit", QueueChannel.class);
		Message<?> replyMessage = queueChannel.receive(0);
		assertThat(replyMessage).isNotNull();
		assertThat(replyMessage.getHeaders().get("bar")).isEqualTo("bar");
		assertThat(replyMessage.getHeaders().get("foo")).isEqualTo("foo"); // copied from request Message
		assertThat(replyMessage.getHeaders().get("foobar")).isNull();
		assertThat(replyMessage.getHeaders().get(AmqpHeaders.DELIVERY_MODE)).isNull();
		assertThat(replyMessage.getHeaders().get(AmqpHeaders.CONTENT_TYPE)).isNull();
		assertThat(replyMessage.getHeaders().get(AmqpHeaders.APP_ID)).isNull();

		shouldBePersistent.set(true);
		message = MessageBuilder.withPayload("hello")
				.setHeader("foo", "foo")
				.setHeader(AmqpHeaders.DELIVERY_MODE, MessageDeliveryMode.PERSISTENT)
				.build();
		requestChannel.send(message);
		replyMessage = queueChannel.receive(0);
		assertThat(replyMessage).isNotNull();
	}

	@Test
	public void withHeaderMapperCustomAndStandardResponse() {
		Object eventDrivenConsumer = this.context.getBean("withHeaderMapperCustomAndStandardResponse");

		AmqpOutboundEndpoint endpoint = TestUtils.getPropertyValue(eventDrivenConsumer, "handler");
		assertThat(TestUtils.<MessageDeliveryMode>getPropertyValue(endpoint, "defaultDeliveryMode")).isNull();
		assertThat(TestUtils.<Boolean>getPropertyValue(endpoint, "headersMappedLast")).isFalse();

		Field amqpTemplateField = ReflectionUtils.findField(AmqpOutboundEndpoint.class, "rabbitTemplate");
		amqpTemplateField.setAccessible(true);
		RabbitTemplate amqpTemplate = TestUtils.getPropertyValue(endpoint, "rabbitTemplate");
		amqpTemplate = Mockito.spy(amqpTemplate);

		Mockito.doAnswer(invocation -> {
					Object[] args = invocation.getArguments();
					org.springframework.amqp.core.Message amqpRequestMessage = (org.springframework.amqp.core.Message) args[2];
					MessageProperties properties = amqpRequestMessage.getMessageProperties();
					assertThat(properties.getHeaders().get("foo")).isEqualTo("foo");
					// mock reply AMQP message
					MessageProperties amqpProperties = new MessageProperties();
					amqpProperties.setAppId("test.appId");
					amqpProperties.setHeader("foobar", "foobar");
					amqpProperties.setHeader("bar", "bar");
					assertThat(properties.getDeliveryMode()).isEqualTo(MessageDeliveryMode.PERSISTENT);
					amqpProperties.setReceivedDeliveryMode(properties.getDeliveryMode());
					return new org.springframework.amqp.core.Message("hello".getBytes(), amqpProperties);
				})
				.when(amqpTemplate).sendAndReceive(Mockito.any(String.class), Mockito.any(String.class),
						Mockito.any(org.springframework.amqp.core.Message.class), isNull());
		ReflectionUtils.setField(amqpTemplateField, endpoint, amqpTemplate);

		MessageChannel requestChannel = this.context.getBean("toRabbit2", MessageChannel.class);
		Message<?> message = MessageBuilder.withPayload("hello").setHeader("foo", "foo").build();
		requestChannel.send(message);

		Mockito.verify(amqpTemplate, Mockito.times(1)).sendAndReceive(Mockito.any(String.class),
				Mockito.any(String.class), Mockito.any(org.springframework.amqp.core.Message.class),
				isNull());

		// verify reply
		QueueChannel queueChannel = this.context.getBean("fromRabbit", QueueChannel.class);
		Message<?> replyMessage = queueChannel.receive(0);
		assertThat(replyMessage.getHeaders().get("bar")).isEqualTo("bar");
		assertThat(replyMessage.getHeaders().get("foo")).isEqualTo("foo"); // copied from request Message
		assertThat(replyMessage.getHeaders().get("foobar")).isNull();
		assertThat(replyMessage.getHeaders().get(AmqpHeaders.RECEIVED_DELIVERY_MODE)).isNotNull();
		assertThat(replyMessage.getHeaders().get(AmqpHeaders.CONTENT_TYPE)).isNotNull();
		assertThat(replyMessage.getHeaders().get(AmqpHeaders.APP_ID)).isNotNull();
	}

	@Test
	public void withHeaderMapperNothingToMap() {
		Object eventDrivenConsumer = this.context.getBean("withHeaderMapperNothingToMap");

		AmqpOutboundEndpoint endpoint = TestUtils.getPropertyValue(eventDrivenConsumer, "handler");

		Field amqpTemplateField = ReflectionUtils.findField(AmqpOutboundEndpoint.class, "rabbitTemplate");
		amqpTemplateField.setAccessible(true);
		RabbitTemplate amqpTemplate = TestUtils.getPropertyValue(endpoint, "rabbitTemplate");
		amqpTemplate = Mockito.spy(amqpTemplate);

		Mockito.doAnswer(invocation -> {
					Object[] args = invocation.getArguments();
					org.springframework.amqp.core.Message amqpRequestMessage = (org.springframework.amqp.core.Message) args[2];
					MessageProperties properties = amqpRequestMessage.getMessageProperties();
					assertThat(properties.getHeaders().get("foo")).isNull();
					// mock reply AMQP message
					MessageProperties amqpProperties = new MessageProperties();
					amqpProperties.setAppId("test.appId");
					amqpProperties.setHeader("foobar", "foobar");
					amqpProperties.setHeader("bar", "bar");
					return new org.springframework.amqp.core.Message("hello".getBytes(), amqpProperties);
				})
				.when(amqpTemplate).sendAndReceive(Mockito.any(String.class), Mockito.any(String.class),
						Mockito.any(org.springframework.amqp.core.Message.class), isNull());
		ReflectionUtils.setField(amqpTemplateField, endpoint, amqpTemplate);

		MessageChannel requestChannel = this.context.getBean("toRabbit3", MessageChannel.class);
		Message<?> message = MessageBuilder.withPayload("hello").setHeader("foo", "foo").build();
		requestChannel.send(message);

		Mockito.verify(amqpTemplate, Mockito.times(1)).sendAndReceive(Mockito.any(String.class),
				Mockito.any(String.class), Mockito.any(org.springframework.amqp.core.Message.class),
				isNull());

		// verify reply
		QueueChannel queueChannel = context.getBean("fromRabbit", QueueChannel.class);
		Message<?> replyMessage = queueChannel.receive(0);
		assertThat(replyMessage.getHeaders().get("bar")).isNull();
		assertThat(replyMessage.getHeaders().get("foo")).isEqualTo("foo"); // copied from request Message
		assertThat(replyMessage.getHeaders().get("foobar")).isNull();
		assertThat(replyMessage.getHeaders().get(AmqpHeaders.DELIVERY_MODE)).isNull();
		assertThat(replyMessage.getHeaders().get(AmqpHeaders.CONTENT_TYPE)).isNull();
		assertThat(replyMessage.getHeaders().get(AmqpHeaders.APP_ID)).isNull();
		assertThat(adviceCalled).isEqualTo(1);
	}

	@Test //INT-1029
	public void amqpOutboundGatewayWithinChain() {
		Object eventDrivenConsumer = this.context.getBean("chainWithRabbitOutboundGateway");

		List<?> chainHandlers = TestUtils.getPropertyValue(eventDrivenConsumer, "handler.handlers");

		AmqpOutboundEndpoint endpoint = (AmqpOutboundEndpoint) chainHandlers.get(0);

		Field amqpTemplateField = ReflectionUtils.findField(AmqpOutboundEndpoint.class, "rabbitTemplate");
		amqpTemplateField.setAccessible(true);
		RabbitTemplate amqpTemplate = TestUtils.getPropertyValue(endpoint, "rabbitTemplate");
		amqpTemplate = Mockito.spy(amqpTemplate);

		Mockito.doAnswer(invocation -> {
					Object[] args = invocation.getArguments();
					org.springframework.amqp.core.Message amqpRequestMessage = (org.springframework.amqp.core.Message) args[2];
					MessageProperties properties = amqpRequestMessage.getMessageProperties();
					assertThat(properties.getHeaders().get("foo")).isNull();
					// mock reply AMQP message
					MessageProperties amqpProperties = new MessageProperties();
					amqpProperties.setAppId("test.appId");
					amqpProperties.setHeader("foobar", "foobar");
					amqpProperties.setHeader("bar", "bar");
					return new org.springframework.amqp.core.Message("hello".getBytes(), amqpProperties);
				})
				.when(amqpTemplate).sendAndReceive(Mockito.any(String.class), Mockito.any(String.class),
						Mockito.any(org.springframework.amqp.core.Message.class), isNull());
		ReflectionUtils.setField(amqpTemplateField, endpoint, amqpTemplate);

		MessageChannel requestChannel = this.context.getBean("toRabbit4", MessageChannel.class);
		Message<?> message = MessageBuilder.withPayload("hello").setHeader("foo", "foo").build();
		requestChannel.send(message);

		Mockito.verify(amqpTemplate, Mockito.times(1)).sendAndReceive(Mockito.any(String.class),
				Mockito.any(String.class), Mockito.any(org.springframework.amqp.core.Message.class),
				isNull());

		// verify reply
		QueueChannel queueChannel = this.context.getBean("fromRabbit", QueueChannel.class);
		Message<?> replyMessage = queueChannel.receive(0);
		assertThat(new String((byte[]) replyMessage.getPayload())).isEqualTo("hello");
		assertThat(replyMessage.getHeaders().get("bar")).isNull();
		assertThat(replyMessage.getHeaders().get("foo")).isEqualTo("foo"); // copied from request Message
		assertThat(replyMessage.getHeaders().get("foobar")).isNull();
		assertThat(replyMessage.getHeaders().get(AmqpHeaders.DELIVERY_MODE)).isNull();
		assertThat(replyMessage.getHeaders().get(AmqpHeaders.CONTENT_TYPE)).isNull();
		assertThat(replyMessage.getHeaders().get(AmqpHeaders.APP_ID)).isNull();

	}

	@Test
	public void testInt2971HeaderMapperAndMappedHeadersExclusivity() {
		try {
			new ClassPathXmlApplicationContext("AmqpOutboundGatewayParserTests-headerMapper-fail-context.xml",
					this.getClass()).close();
		}
		catch (BeanDefinitionParsingException e) {
			assertThat(e.getMessage().startsWith("Configuration problem: The 'header-mapper' attribute " +
					"is mutually exclusive with 'mapped-request-headers' or 'mapped-reply-headers'")).isTrue();
		}
	}

	public static class FooAdvice extends AbstractRequestHandlerAdvice {

		@Override
		protected Object doInvoke(ExecutionCallback callback, Object target, Message<?> message) {
			adviceCalled++;
			return callback.execute();
		}

	}

}
