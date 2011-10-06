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
package org.springframework.integration.amqp.config;

import java.lang.reflect.Field;

import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.amqp.AmqpHeaders;
import org.springframework.integration.amqp.outbound.AmqpOutboundEndpoint;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.util.ReflectionUtils;

import static junit.framework.Assert.assertEquals;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Oleg Zhurakousky
 *
 */
public class AmqpOutboundGatewayParserTests {

	@Test
	public void testGatewayConfig(){
		ApplicationContext context = new ClassPathXmlApplicationContext("AmqpOutboundGatewayParserTests-context.xml", this.getClass());
		Object edc = context.getBean("rabbitGateway");
		AmqpOutboundEndpoint gateway = TestUtils.getPropertyValue(edc, "handler", AmqpOutboundEndpoint.class);
		assertEquals(5, gateway.getOrder());
		assertTrue(context.containsBean("rabbitGateway"));
		assertEquals(context.getBean("fromRabbit"), TestUtils.getPropertyValue(gateway, "outputChannel"));
	}
	
	@SuppressWarnings("rawtypes")
	@Test
	public void withHeaderMapperCustomRequestResponse() {
		ApplicationContext context = new ClassPathXmlApplicationContext("AmqpOutboundGatewayParserTests-context.xml", this.getClass());
		Object eventDrivernConsumer = context.getBean("withHeaderMapperCustomRequestResponse");
		
		AmqpOutboundEndpoint endpoint = TestUtils.getPropertyValue(eventDrivernConsumer, "handler", AmqpOutboundEndpoint.class);
		
		Field amqpTemplateField = ReflectionUtils.findField(AmqpOutboundEndpoint.class, "amqpTemplate");
		amqpTemplateField.setAccessible(true);
		RabbitTemplate amqpTemplate = TestUtils.getPropertyValue(endpoint, "amqpTemplate", RabbitTemplate.class);
		amqpTemplate = Mockito.spy(amqpTemplate);
		
		Mockito.doAnswer(new Answer() {
		      public Object answer(InvocationOnMock invocation) {
		          Object[] args = invocation.getArguments();
		          org.springframework.amqp.core.Message amqpRequestMessage = (org.springframework.amqp.core.Message) args[2];
		          MessageProperties properties = amqpRequestMessage.getMessageProperties();
		          assertEquals("foo", properties.getHeaders().get("foo"));
		          // mock reply AMQP message
		          MessageProperties amqpProperties = new MessageProperties();
		  		  amqpProperties.setAppId("test.appId");
		  		  amqpProperties.setHeader("foobar", "foobar");
		  		  amqpProperties.setHeader("bar", "bar");
		          org.springframework.amqp.core.Message amqpReplyMessage = new org.springframework.amqp.core.Message("hello".getBytes(), amqpProperties);
		          return amqpReplyMessage;
		      }})
		 .when(amqpTemplate).sendAndReceive(Mockito.any(String.class), Mockito.any(String.class), Mockito.any(org.springframework.amqp.core.Message.class));
		ReflectionUtils.setField(amqpTemplateField, endpoint, amqpTemplate);

		MessageChannel requestChannel = context.getBean("toRabbit", MessageChannel.class);
		Message<?> message = MessageBuilder.withPayload("hello").setHeader("foo", "foo").build();
		requestChannel.send(message);
		
		Mockito.verify(amqpTemplate, Mockito.times(1)).sendAndReceive(Mockito.any(String.class), Mockito.any(String.class), Mockito.any(org.springframework.amqp.core.Message.class));
		
		// verify reply
		QueueChannel queueChannel = context.getBean("fromRabbit", QueueChannel.class);
		Message<?> replyMessage = queueChannel.receive(0);
		System.out.println(replyMessage);
		assertEquals("bar", replyMessage.getHeaders().get("bar"));
		assertEquals("foo", replyMessage.getHeaders().get("foo")); // copied from request Message
		assertNull(replyMessage.getHeaders().get("foobar"));
		assertNull(replyMessage.getHeaders().get(AmqpHeaders.DELIVERY_MODE));
		assertNull(replyMessage.getHeaders().get(AmqpHeaders.CONTENT_TYPE));
		assertNull(replyMessage.getHeaders().get(AmqpHeaders.APP_ID));
	}
	
	@SuppressWarnings("rawtypes")
	@Test
	public void withHeaderMapperCustomAndStandardResponse() {
		ApplicationContext context = new ClassPathXmlApplicationContext("AmqpOutboundGatewayParserTests-context.xml", this.getClass());
		Object eventDrivernConsumer = context.getBean("withHeaderMapperCustomAndStandardResponse");
		
		AmqpOutboundEndpoint endpoint = TestUtils.getPropertyValue(eventDrivernConsumer, "handler", AmqpOutboundEndpoint.class);
		
		Field amqpTemplateField = ReflectionUtils.findField(AmqpOutboundEndpoint.class, "amqpTemplate");
		amqpTemplateField.setAccessible(true);
		RabbitTemplate amqpTemplate = TestUtils.getPropertyValue(endpoint, "amqpTemplate", RabbitTemplate.class);
		amqpTemplate = Mockito.spy(amqpTemplate);
		
		Mockito.doAnswer(new Answer() {
		      public Object answer(InvocationOnMock invocation) {
		          Object[] args = invocation.getArguments();
		          org.springframework.amqp.core.Message amqpRequestMessage = (org.springframework.amqp.core.Message) args[2];
		          MessageProperties properties = amqpRequestMessage.getMessageProperties();
		          assertEquals("foo", properties.getHeaders().get("foo"));
		          // mock reply AMQP message
		          MessageProperties amqpProperties = new MessageProperties();
		  		  amqpProperties.setAppId("test.appId");
		  		  amqpProperties.setHeader("foobar", "foobar");
		  		  amqpProperties.setHeader("bar", "bar");
		          org.springframework.amqp.core.Message amqpReplyMessage = new org.springframework.amqp.core.Message("hello".getBytes(), amqpProperties);
		          return amqpReplyMessage;
		      }})
		 .when(amqpTemplate).sendAndReceive(Mockito.any(String.class), Mockito.any(String.class), Mockito.any(org.springframework.amqp.core.Message.class));
		ReflectionUtils.setField(amqpTemplateField, endpoint, amqpTemplate);

		MessageChannel requestChannel = context.getBean("toRabbit", MessageChannel.class);
		Message<?> message = MessageBuilder.withPayload("hello").setHeader("foo", "foo").build();
		requestChannel.send(message);
		
		Mockito.verify(amqpTemplate, Mockito.times(1)).sendAndReceive(Mockito.any(String.class), Mockito.any(String.class), Mockito.any(org.springframework.amqp.core.Message.class));
		
		// verify reply
		QueueChannel queueChannel = context.getBean("fromRabbit", QueueChannel.class);
		Message<?> replyMessage = queueChannel.receive(0);
		assertEquals("bar", replyMessage.getHeaders().get("bar"));
		assertEquals("foo", replyMessage.getHeaders().get("foo")); // copied from request Message
		assertNull(replyMessage.getHeaders().get("foobar"));
		assertNotNull(replyMessage.getHeaders().get(AmqpHeaders.DELIVERY_MODE));
		assertNotNull(replyMessage.getHeaders().get(AmqpHeaders.CONTENT_TYPE));
		assertNotNull(replyMessage.getHeaders().get(AmqpHeaders.APP_ID));
	}
	
	@SuppressWarnings("rawtypes")
	@Test
	public void withHeaderMapperNothingToMap() {
		ApplicationContext context = new ClassPathXmlApplicationContext("AmqpOutboundGatewayParserTests-context.xml", this.getClass());
		Object eventDrivernConsumer = context.getBean("withHeaderMapperNothingToMap");
		
		AmqpOutboundEndpoint endpoint = TestUtils.getPropertyValue(eventDrivernConsumer, "handler", AmqpOutboundEndpoint.class);
		
		Field amqpTemplateField = ReflectionUtils.findField(AmqpOutboundEndpoint.class, "amqpTemplate");
		amqpTemplateField.setAccessible(true);
		RabbitTemplate amqpTemplate = TestUtils.getPropertyValue(endpoint, "amqpTemplate", RabbitTemplate.class);
		amqpTemplate = Mockito.spy(amqpTemplate);
		
		Mockito.doAnswer(new Answer() {
		      public Object answer(InvocationOnMock invocation) {
		          Object[] args = invocation.getArguments();
		          org.springframework.amqp.core.Message amqpRequestMessage = (org.springframework.amqp.core.Message) args[2];
		          MessageProperties properties = amqpRequestMessage.getMessageProperties();
		          assertNull(properties.getHeaders().get("foo"));
		          // mock reply AMQP message
		          MessageProperties amqpProperties = new MessageProperties();
		  		  amqpProperties.setAppId("test.appId");
		  		  amqpProperties.setHeader("foobar", "foobar");
		  		  amqpProperties.setHeader("bar", "bar");
		          org.springframework.amqp.core.Message amqpReplyMessage = new org.springframework.amqp.core.Message("hello".getBytes(), amqpProperties);
		          return amqpReplyMessage;
		      }})
		 .when(amqpTemplate).sendAndReceive(Mockito.any(String.class), Mockito.any(String.class), Mockito.any(org.springframework.amqp.core.Message.class));
		ReflectionUtils.setField(amqpTemplateField, endpoint, amqpTemplate);

		MessageChannel requestChannel = context.getBean("toRabbit", MessageChannel.class);
		Message<?> message = MessageBuilder.withPayload("hello").setHeader("foo", "foo").build();
		requestChannel.send(message);
		
		Mockito.verify(amqpTemplate, Mockito.times(1)).sendAndReceive(Mockito.any(String.class), Mockito.any(String.class), Mockito.any(org.springframework.amqp.core.Message.class));
		
		// verify reply
		QueueChannel queueChannel = context.getBean("fromRabbit", QueueChannel.class);
		Message<?> replyMessage = queueChannel.receive(0);
		assertNull(replyMessage.getHeaders().get("bar"));
		assertEquals("foo", replyMessage.getHeaders().get("foo")); // copied from request Message
		assertNull(replyMessage.getHeaders().get("foobar"));
		assertNull(replyMessage.getHeaders().get(AmqpHeaders.DELIVERY_MODE));
		assertNull(replyMessage.getHeaders().get(AmqpHeaders.CONTENT_TYPE));
		assertNull(replyMessage.getHeaders().get(AmqpHeaders.APP_ID));
	}
}
