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

package org.springframework.integration.redis.config;

import static org.junit.Assert.*;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.expression.Expression;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.handler.advice.RequestHandlerRetryAdvice;
import org.springframework.integration.redis.inbound.RedisInboundChannelAdapter;
import org.springframework.integration.redis.outbound.RedisPublishingMessageHandler;
import org.springframework.integration.redis.rules.RedisAvailable;
import org.springframework.integration.redis.rules.RedisAvailableTests;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.converter.SimpleMessageConverter;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Gunnar Hillert
 * @author Gary Russell
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class RedisOutboundChannelAdapterParserTests extends RedisAvailableTests {

	@Autowired
	private ApplicationContext context;

	@Autowired
	private RedisInboundChannelAdapter fooInbound;

	@Test
	@RedisAvailable
	public void validateConfiguration() {
		EventDrivenConsumer adapter = context.getBean("outboundAdapter", EventDrivenConsumer.class);
		Object handler = context.getBean("outboundAdapter.handler");

		assertEquals("outboundAdapter", adapter.getComponentName());
		DirectFieldAccessor accessor = new DirectFieldAccessor(handler);
		Object topicExpression = accessor.getPropertyValue("topicExpression");
		assertNotNull(topicExpression);
		assertEquals("headers['topic'] ?: 'foo'", ((Expression) topicExpression).getExpressionString());
		Object converterBean = context.getBean("testConverter");
		assertEquals(converterBean, accessor.getPropertyValue("messageConverter"));
		assertEquals(context.getBean("serializer"), accessor.getPropertyValue("serializer"));
		Object mbf = context.getBean(IntegrationUtils.INTEGRATION_MESSAGE_BUILDER_FACTORY_BEAN_NAME);
		assertSame(mbf, TestUtils.getPropertyValue(handler, "messageConverter.messageBuilderFactory"));

		Object endpointHandler = TestUtils.getPropertyValue(adapter, "handler");

		assertTrue(AopUtils.isAopProxy(endpointHandler));

		assertThat(TestUtils.getPropertyValue(endpointHandler, "h.advised.advisors.first.item.advice"),
				Matchers.instanceOf(RequestHandlerRetryAdvice.class));
	}

	@Test
	@RedisAvailable
	public void testOutboundChannelAdapterMessaging() throws Exception{
		MessageChannel sendChannel = context.getBean("sendChannel", MessageChannel.class);
		this.awaitContainerSubscribed(TestUtils.getPropertyValue(fooInbound, "container",
				RedisMessageListenerContainer.class));
		sendChannel.send(new GenericMessage<String>("Hello Redis"));
		QueueChannel receiveChannel = context.getBean("receiveChannel", QueueChannel.class);
		Message<?> message = receiveChannel.receive(5000);
		assertNotNull(message);
		assertEquals("Hello Redis", message.getPayload());

		sendChannel = context.getBean("sendChannel", MessageChannel.class);
		sendChannel.send(MessageBuilder.withPayload("Hello Redis").setHeader("topic", "bar").build());
		receiveChannel = context.getBean("barChannel", QueueChannel.class);
		message = receiveChannel.receive(5000);
		assertNotNull(message);
		assertEquals("Hello Redis", message.getPayload());
	}

	@Test //INT-2275
	@RedisAvailable
	public void testOutboundChannelAdapterWithinChain() throws Exception{
		MessageChannel sendChannel = context.getBean("redisOutboudChain", MessageChannel.class);
		this.awaitContainerSubscribed(TestUtils.getPropertyValue(fooInbound, "container",
				RedisMessageListenerContainer.class));
		sendChannel.send(new GenericMessage<String>("Hello Redis from chain"));
		QueueChannel receiveChannel = context.getBean("receiveChannel", QueueChannel.class);
		Message<?> message = receiveChannel.receive(5000);
		assertNotNull(message);
		assertEquals("Hello Redis from chain", message.getPayload());
	}


	@SuppressWarnings("unused")
	private static class TestMessageConverter extends SimpleMessageConverter {
	}

}
