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

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.amqp.core.Address;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.amqp.inbound.AmqpInboundGateway;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;

/**
 * @author Mark Fisher
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Gary Russell
 * @author Glenn Renfro
 *
 * @since 2.1
 */
@SpringJUnitConfig
@DirtiesContext
public class AmqpInboundGatewayParserTests {

	@Autowired
	private ApplicationContext context;

	@Test
	public void customMessageConverter() {
		Object gateway = context.getBean("gateway");
		MessageConverter gatewayConverter =
				TestUtils.<MessageConverter>getPropertyValue(gateway, "amqpMessageConverter");
		MessageConverter templateConverter =
				TestUtils.<MessageConverter>getPropertyValue(gateway, "amqpTemplate.messageConverter");
		TestConverter testConverter = context.getBean("testConverter", TestConverter.class);
		assertThat(gatewayConverter).isSameAs(testConverter);
		assertThat(templateConverter).isSameAs(testConverter);
		assertThat(TestUtils.<Boolean>getPropertyValue(gateway, "autoStartup")).isEqualTo(Boolean.TRUE);
		assertThat(TestUtils.<Integer>getPropertyValue(gateway, "phase")).isEqualTo(0);
		assertThat(TestUtils.<Long>getPropertyValue(gateway, "messagingTemplate.receiveTimeout")).isEqualTo(1234L);
		assertThat(TestUtils.<Boolean>getPropertyValue(gateway, "messageListenerContainer.missingQueuesFatal"))
				.isTrue();
	}

	@Test
	public void verifyLifeCycle() {
		Object gateway = context.getBean("autoStartFalseGateway");
		assertThat(TestUtils.<Boolean>getPropertyValue(gateway, "autoStartup")).isEqualTo(Boolean.FALSE);
		assertThat(TestUtils.<Integer>getPropertyValue(gateway, "phase")).isEqualTo(123);
		assertThat(TestUtils.<Boolean>getPropertyValue(gateway, "messageListenerContainer.missingQueuesFatal"))
				.isFalse();
		Object amqpTemplate = context.getBean("amqpTemplate");
		assertThat(TestUtils.<AmqpTemplate>getPropertyValue(gateway, "amqpTemplate")).isSameAs(amqpTemplate);
		Address defaultReplyTo = TestUtils.getPropertyValue(gateway, "defaultReplyTo");
		Address expected = new Address("fooExchange/barRoutingKey");
		assertThat(defaultReplyTo.getExchangeName()).isEqualTo(expected.getExchangeName());
		assertThat(defaultReplyTo.getRoutingKey()).isEqualTo(expected.getRoutingKey());
		assertThat(defaultReplyTo).isEqualTo(expected);
	}

	@Test
	public void verifyUsageWithHeaderMapper() throws Exception {
		DirectChannel requestChannel = context.getBean("requestChannel", DirectChannel.class);
		requestChannel.subscribe(siMessage -> {
			org.springframework.messaging.Message<?> replyMessage = MessageBuilder.fromMessage(siMessage)
					.setHeader("bar", "bar").build();
			MessageChannel replyChannel = (MessageChannel) siMessage.getHeaders().getReplyChannel();
			replyChannel.send(replyMessage);
		});

		final AmqpInboundGateway gateway = context.getBean("withHeaderMapper", AmqpInboundGateway.class);
		assertThat(TestUtils.<Boolean>getPropertyValue(gateway, "replyHeadersMappedLast")).isTrue();
		Field amqpTemplateField = ReflectionUtils.findField(AmqpInboundGateway.class, "amqpTemplate");
		amqpTemplateField.setAccessible(true);
		RabbitTemplate amqpTemplate = TestUtils.getPropertyValue(gateway, "amqpTemplate");
		amqpTemplate = Mockito.spy(amqpTemplate);

		Mockito.doAnswer(invocation -> {
			Object[] args = invocation.getArguments();
			Message amqpReplyMessage = (Message) args[2];
			MessageProperties properties = amqpReplyMessage.getMessageProperties();
			assertThat(properties.getHeaders().get("bar")).isEqualTo("bar");
			return null;
		}).when(amqpTemplate).send(Mockito.any(String.class), Mockito.any(String.class),
				Mockito.any(Message.class), isNull());
		ReflectionUtils.setField(amqpTemplateField, gateway, amqpTemplate);

		AbstractMessageListenerContainer mlc =
				TestUtils.<AbstractMessageListenerContainer>getPropertyValue(gateway, "messageListenerContainer");
		ChannelAwareMessageListener listener = TestUtils.getPropertyValue(mlc, "messageListener");
		MessageProperties amqpProperties = new MessageProperties();
		amqpProperties.setAppId("test.appId");
		amqpProperties.setClusterId("test.clusterId");
		amqpProperties.setContentEncoding("test.contentEncoding");
		amqpProperties.setContentLength(99L);
		amqpProperties.setReplyTo("oleg");
		amqpProperties.setContentType("test.contentType");
		amqpProperties.setHeader("foo", "foo");
		amqpProperties.setHeader("bar", "bar");
		Message amqpMessage = new Message("hello".getBytes(), amqpProperties);
		listener.onMessage(amqpMessage, mock());

		Mockito.verify(amqpTemplate, Mockito.times(1)).send(Mockito.any(String.class), Mockito.any(String.class),
				Mockito.any(Message.class), isNull());
	}

	@Test
	public void testInt2971HeaderMapperAndMappedHeadersExclusivity() {
		assertThatExceptionOfType(BeanDefinitionParsingException.class)
				.isThrownBy(() ->
						new ClassPathXmlApplicationContext("AmqpInboundGatewayParserTests-headerMapper-fail-context.xml",
								getClass()))
				.withMessageStartingWith("Configuration problem: The 'header-mapper' attribute " +
						"is mutually exclusive with 'mapped-request-headers' or 'mapped-reply-headers'");
	}

	private static class TestConverter extends SimpleMessageConverter {

	}

}
