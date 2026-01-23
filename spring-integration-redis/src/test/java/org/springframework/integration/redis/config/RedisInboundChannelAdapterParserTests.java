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

package org.springframework.integration.redis.config;

import java.util.concurrent.Executor;

import org.junit.jupiter.api.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.redis.RedisContainerTest;
import org.springframework.integration.redis.inbound.RedisInboundChannelAdapter;
import org.springframework.integration.support.converter.SimpleMessageConverter;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Gary Russell
 * @author Gunnar Hillert
 * @author Venil Noronha
 * @author Artem Bilan
 * @author Artem Vozhdayenko
 * @author Glenn Renfro
 */
@SpringJUnitConfig
@DirtiesContext
class RedisInboundChannelAdapterParserTests implements RedisContainerTest {

	@Autowired
	private ApplicationContext context;

	@Autowired
	private MessageChannel autoChannel;

	@Autowired
	@Qualifier("autoChannel.adapter")
	private RedisInboundChannelAdapter autoChannelAdapter;

	@Autowired
	private Executor executor;

	@Autowired
	private RedisConnectionFactory redisConnectionFactory;

	@Test
	void validateConfiguration() {
		RedisInboundChannelAdapter adapter = context.getBean("adapter", RedisInboundChannelAdapter.class);
		assertThat(adapter.getComponentName()).isEqualTo("adapter");
		assertThat(adapter.getComponentType()).isEqualTo("redis:inbound-channel-adapter");
		DirectFieldAccessor accessor = new DirectFieldAccessor(adapter);
		Object errorChannelBean = context.getBean("testErrorChannel");
		assertThat(accessor.getPropertyValue("errorChannel")).isEqualTo(errorChannelBean);
		Object converterBean = context.getBean("testConverter");
		assertThat(accessor.getPropertyValue("messageConverter")).isEqualTo(converterBean);
		assertThat(accessor.getPropertyValue("serializer")).isEqualTo(context.getBean("serializer"));

		Object container = accessor.getPropertyValue("container");
		DirectFieldAccessor containerAccessor = new DirectFieldAccessor(container);
		assertThat(containerAccessor.getPropertyValue("taskExecutor")).isSameAs(this.executor);

		Object bean = context.getBean("withoutSerializer.adapter");
		assertThat(bean).isNotNull();
		assertThat(TestUtils.<RedisInboundChannelAdapter>getPropertyValue(bean, "serializer")).isNull();
	}

	@Test
	void testInboundChannelAdapterMessaging() throws Exception {
		RedisInboundChannelAdapter adapter = context.getBean("adapter", RedisInboundChannelAdapter.class);
		adapter.start();
		RedisContainerTest.awaitContainerSubscribedWithPatterns(TestUtils.getPropertyValue(adapter, "container"));

		redisConnectionFactory.getConnection().publish("foo".getBytes(), "Hello Redis from foo".getBytes());
		redisConnectionFactory.getConnection().publish("bar".getBytes(), "Hello Redis from bar".getBytes());

		QueueChannel receiveChannel = context.getBean("receiveChannel", QueueChannel.class);
		for (int i = 0; i < 3; i++) {
			Message<?> receive = receiveChannel.receive(10000);
			assertThat(receive).isNotNull();
			assertThat(receive.getPayload()).isIn("Hello Redis from foo", "Hello Redis from bar");
		}

		adapter.stop();
	}

	@Test
	void testAutoChannel() {
		assertThat(TestUtils.<MessageChannel>getPropertyValue(autoChannelAdapter, "outputChannel"))
				.isSameAs(autoChannel);
	}

	@SuppressWarnings("unused")
	private static class TestMessageConverter extends SimpleMessageConverter {

	}

}
