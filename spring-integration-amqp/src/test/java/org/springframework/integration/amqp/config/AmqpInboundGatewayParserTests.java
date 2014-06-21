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

package org.springframework.integration.amqp.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Field;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.ChannelAwareMessageListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer;
import org.springframework.amqp.rabbit.support.CorrelationData;
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
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.ReflectionUtils;

/**
 * @author Mark Fisher
 * @author Gunnar Hillert
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 2.1
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class AmqpInboundGatewayParserTests {

	@Autowired
	private ApplicationContext context;

	@Test
	public void customMessageConverter() {
		Object gateway = context.getBean("gateway");
		MessageConverter gatewayConverter = TestUtils.getPropertyValue(gateway, "amqpMessageConverter", MessageConverter.class);
		MessageConverter templateConverter = TestUtils.getPropertyValue(gateway, "amqpTemplate.messageConverter", MessageConverter.class);
		TestConverter testConverter = context.getBean("testConverter", TestConverter.class);
		assertSame(testConverter, gatewayConverter);
		assertSame(testConverter, templateConverter);
		assertEquals(Boolean.TRUE, TestUtils.getPropertyValue(gateway, "autoStartup"));
		assertEquals(0, TestUtils.getPropertyValue(gateway, "phase"));
		assertEquals(Long.valueOf(1234L), TestUtils.getPropertyValue(gateway, "replyTimeout", Long.class));
		assertEquals(Long.valueOf(1234L), TestUtils.getPropertyValue(gateway, "messagingTemplate.receiveTimeout", Long.class));
		assertTrue(TestUtils.getPropertyValue(gateway, "messageListenerContainer.missingQueuesFatal", Boolean.class));
	}

	@Test
	public void verifyLifeCycle() {
		Object gateway = context.getBean("autoStartFalseGateway");
		assertEquals(Boolean.FALSE, TestUtils.getPropertyValue(gateway, "autoStartup"));
		assertEquals(123, TestUtils.getPropertyValue(gateway, "phase"));
		assertFalse(TestUtils.getPropertyValue(gateway, "messageListenerContainer.missingQueuesFatal", Boolean.class));
	}

	@SuppressWarnings("rawtypes")
	@Test
	public void verifyUsageWithHeaderMapper() throws Exception{
		DirectChannel requestChannel = context.getBean("requestChannel", DirectChannel.class);
		requestChannel.subscribe(new MessageHandler() {
			@Override
			public void handleMessage(org.springframework.messaging.Message<?> siMessage)
					throws MessagingException {
				org.springframework.messaging.Message<?> replyMessage = MessageBuilder.fromMessage(siMessage).setHeader("bar", "bar").build();
				MessageChannel replyChannel = (MessageChannel) siMessage.getHeaders().getReplyChannel();
				replyChannel.send(replyMessage);
			}
		});

		final AmqpInboundGateway gateway = context.getBean("withHeaderMapper", AmqpInboundGateway.class);

		Field amqpTemplateField = ReflectionUtils.findField(AmqpInboundGateway.class, "amqpTemplate");
		amqpTemplateField.setAccessible(true);
		RabbitTemplate amqpTemplate = TestUtils.getPropertyValue(gateway, "amqpTemplate", RabbitTemplate.class);
		amqpTemplate = Mockito.spy(amqpTemplate);

		Mockito.doAnswer(new Answer() {
			@Override
			public Object answer(InvocationOnMock invocation) {
				Object[] args = invocation.getArguments();
				Message amqpReplyMessage = (Message) args[2];
				MessageProperties properties = amqpReplyMessage.getMessageProperties();
				assertEquals("bar", properties.getHeaders().get("bar"));
				return null;
			}})
				.when(amqpTemplate).send(Mockito.any(String.class), Mockito.any(String.class),
				Mockito.any(Message.class), Mockito.any(CorrelationData.class));
		ReflectionUtils.setField(amqpTemplateField, gateway, amqpTemplate);

		AbstractMessageListenerContainer mlc =
				TestUtils.getPropertyValue(gateway, "messageListenerContainer", AbstractMessageListenerContainer.class);
		ChannelAwareMessageListener listener = TestUtils.getPropertyValue(mlc, "messageListener",
				ChannelAwareMessageListener.class);
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
		listener.onMessage(amqpMessage, null);

		Mockito.verify(amqpTemplate, Mockito.times(1)).send(Mockito.any(String.class), Mockito.any(String.class),
				Mockito.any(Message.class), Mockito.any(CorrelationData.class));
	}

	@Test
	public void testInt2971HeaderMapperAndMappedHeadersExclusivity() {
		try {
			new ClassPathXmlApplicationContext("AmqpInboundGatewayParserTests-headerMapper-fail-context.xml", this.getClass());
		}
		catch (BeanDefinitionParsingException e) {
			assertTrue(e.getMessage().startsWith("Configuration problem: The 'header-mapper' attribute " +
					"is mutually exclusive with 'mapped-request-headers' or 'mapped-reply-headers'"));
		}
	}

	private static class TestConverter extends SimpleMessageConverter {}

}
