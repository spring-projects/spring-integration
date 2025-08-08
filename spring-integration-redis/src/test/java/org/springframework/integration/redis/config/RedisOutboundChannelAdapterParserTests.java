/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.redis.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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
import org.springframework.integration.redis.RedisContainerTest;
import org.springframework.integration.redis.inbound.RedisInboundChannelAdapter;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.converter.SimpleMessageConverter;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author Artem Vozhdayenko
 */
@SpringJUnitConfig
@DirtiesContext
class RedisOutboundChannelAdapterParserTests implements RedisContainerTest {

	@Autowired
	private ApplicationContext context;

	@Autowired
	private RedisInboundChannelAdapter fooInbound;

	@Autowired
	private RedisInboundChannelAdapter barInbound;

	@BeforeEach
	void setup() {
		this.fooInbound.start();
		this.barInbound.start();
	}

	@AfterEach
	void tearDown() {
		this.fooInbound.stop();
		this.barInbound.stop();
	}

	@Test
	void validateConfiguration() {
		EventDrivenConsumer adapter = context.getBean("outboundAdapter", EventDrivenConsumer.class);
		Object handler = context.getBean("outboundAdapter.handler");

		assertThat(adapter.getComponentName()).isEqualTo("outboundAdapter");
		DirectFieldAccessor accessor = new DirectFieldAccessor(handler);
		Object topicExpression = accessor.getPropertyValue("topicExpression");
		assertThat(topicExpression).isNotNull();
		assertThat(((Expression) topicExpression).getExpressionString()).isEqualTo("headers['topic'] ?: 'foo'");
		Object converterBean = context.getBean("testConverter");
		assertThat(accessor.getPropertyValue("messageConverter")).isEqualTo(converterBean);
		assertThat(accessor.getPropertyValue("serializer")).isEqualTo(context.getBean("serializer"));

		Object endpointHandler = TestUtils.getPropertyValue(adapter, "handler");

		assertThat(AopUtils.isAopProxy(endpointHandler)).isTrue();

		assertThat(((Advised) endpointHandler).getAdvisors()[0].getAdvice())
				.isInstanceOf(RequestHandlerRetryAdvice.class);
	}

	@Test
	void testOutboundChannelAdapterMessaging() throws Exception {
		MessageChannel sendChannel = context.getBean("sendChannel", MessageChannel.class);
		RedisContainerTest.awaitContainerSubscribed(TestUtils.getPropertyValue(fooInbound, "container",
				RedisMessageListenerContainer.class));
		sendChannel.send(new GenericMessage<>("Hello Redis"));
		QueueChannel receiveChannel = context.getBean("receiveChannel", QueueChannel.class);
		Message<?> message = receiveChannel.receive(10000);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isEqualTo("Hello Redis");

		sendChannel = context.getBean("sendChannel", MessageChannel.class);
		sendChannel.send(MessageBuilder.withPayload("Hello Redis").setHeader("topic", "bar").build());
		receiveChannel = context.getBean("barChannel", QueueChannel.class);
		message = receiveChannel.receive(10000);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isEqualTo("Hello Redis");
	}

	@Test
		//INT-2275
	void testOutboundChannelAdapterWithinChain() throws Exception {
		MessageChannel sendChannel = context.getBean("redisOutboundChain", MessageChannel.class);
		RedisContainerTest.awaitContainerSubscribed(TestUtils.getPropertyValue(fooInbound, "container",
				RedisMessageListenerContainer.class));
		sendChannel.send(new GenericMessage<>("Hello Redis from chain"));
		QueueChannel receiveChannel = context.getBean("receiveChannel", QueueChannel.class);
		Message<?> message = receiveChannel.receive(10000);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isEqualTo("Hello Redis from chain");
	}

	@SuppressWarnings("unused")
	private static class TestMessageConverter extends SimpleMessageConverter {

	}

}
