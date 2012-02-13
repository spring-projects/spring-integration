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
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.amqp.outbound.AmqpOutboundEndpoint;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageHandler;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.util.ReflectionUtils;

import static org.junit.Assert.assertNull;

import static junit.framework.Assert.assertEquals;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @since 2.1
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class AmqpOutboundChannelAdapterParserTests {

	@Autowired
	private ApplicationContext context;

	@Test
	public void verifyIdAsChannel() {
		Object channel = context.getBean("rabbitOutbound");
		Object adapter = context.getBean("rabbitOutbound.adapter");
		assertEquals(DirectChannel.class, channel.getClass());
		assertEquals(EventDrivenConsumer.class, adapter.getClass());
		MessageHandler handler = TestUtils.getPropertyValue(adapter, "handler", MessageHandler.class);
		assertEquals(AmqpOutboundEndpoint.class, handler.getClass());
		assertEquals("amqp:outbound-channel-adapter", ((AmqpOutboundEndpoint) handler).getComponentType());
	}
	
	@SuppressWarnings("rawtypes")
	@Test
	public void withHeaderMapperCustomHeaders() {
		Object eventDrivernConsumer = context.getBean("withHeaderMapperCustomHeaders");
		
		AmqpOutboundEndpoint endpoint = TestUtils.getPropertyValue(eventDrivernConsumer, "handler", AmqpOutboundEndpoint.class);
		
		Field amqpTemplateField = ReflectionUtils.findField(AmqpOutboundEndpoint.class, "amqpTemplate");
		amqpTemplateField.setAccessible(true);
		RabbitTemplate amqpTemplate = TestUtils.getPropertyValue(endpoint, "amqpTemplate", RabbitTemplate.class);
		amqpTemplate = Mockito.spy(amqpTemplate);

		Mockito.doAnswer(new Answer() {
		      public Object answer(InvocationOnMock invocation) {
		          Object[] args = invocation.getArguments();
		          org.springframework.amqp.core.Message amqpReplyMessage = (org.springframework.amqp.core.Message) args[2];
		          MessageProperties properties = amqpReplyMessage.getMessageProperties();
		          assertEquals("foo", properties.getHeaders().get("foo"));
		          assertEquals("foobar", properties.getHeaders().get("foobar"));
		          assertNull(properties.getHeaders().get("bar"));
		          return null;
		      }})
		 .when(amqpTemplate).send(Mockito.any(String.class), Mockito.any(String.class), Mockito.any(org.springframework.amqp.core.Message.class));
		ReflectionUtils.setField(amqpTemplateField, endpoint, amqpTemplate);
		
		
		MessageChannel requestChannel = context.getBean("requestChannel", MessageChannel.class);
		Message<?> message = MessageBuilder.withPayload("hello").setHeader("foo", "foo").setHeader("bar", "bar").setHeader("foobar", "foobar").build();
		requestChannel.send(message);
		Mockito.verify(amqpTemplate, Mockito.times(1)).send(Mockito.any(String.class), Mockito.any(String.class), Mockito.any(org.springframework.amqp.core.Message.class));
	}

}
