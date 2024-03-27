/*
 * Copyright 2013-2024 the original author or authors.
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

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.BoundListOperations;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.integration.redis.inbound.RedisQueueMessageDrivenEndpoint;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.util.ErrorHandlingTaskExecutor;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Artem Bilan
 * @author Gary Russell
 * @author Rainer Frey
 * @author Matthias Jeschke
 *
 * @since 3.0
 */
@SpringJUnitConfig
@DirtiesContext
public class RedisQueueInboundChannelAdapterParserTests {

	@Autowired
	@Qualifier("redisConnectionFactory")
	private RedisConnectionFactory connectionFactory;

	@Autowired
	@Qualifier("customRedisConnectionFactory")
	private RedisConnectionFactory customRedisConnectionFactory;

	@Autowired
	@Qualifier("defaultAdapter.adapter")
	private RedisQueueMessageDrivenEndpoint defaultAdapter;

	@Autowired
	@Qualifier("defaultAdapter")
	private MessageChannel defaultAdapterChannel;

	@Autowired
	@Qualifier("customAdapter")
	private RedisQueueMessageDrivenEndpoint customAdapter;

	@Autowired
	@Qualifier("zeroReceiveTimeoutAdapter")
	private RedisQueueMessageDrivenEndpoint zeroReceiveTimeoutAdapter;

	@Autowired
	@Qualifier("errorChannel")
	private MessageChannel errorChannel;

	@Autowired
	@Qualifier("sendChannel")
	private MessageChannel sendChannel;

	@Autowired
	@Qualifier("executor")
	private TaskExecutor taskExecutor;

	@Autowired
	private RedisSerializer<?> serializer;

	@Test
	@SuppressWarnings("unchecked")
	public void testInt3017DefaultConfig() {
		BoundListOperations<String, byte[]> boundListOperations =
				TestUtils.getPropertyValue(this.defaultAdapter, "boundListOperations", BoundListOperations.class);
		assertThat(boundListOperations.getKey()).isEqualTo("si.test.Int3017.Inbound1");
		assertThat(TestUtils.getPropertyValue(this.defaultAdapter, "expectMessage", Boolean.class)).isFalse();
		assertThat(TestUtils.getPropertyValue(this.defaultAdapter, "receiveTimeout")).isEqualTo(1000L);
		assertThat(TestUtils.getPropertyValue(this.defaultAdapter, "recoveryInterval")).isEqualTo(5000L);
		assertThat(TestUtils.getPropertyValue(this.defaultAdapter, "errorChannel")).isNull();
		assertThat(TestUtils.getPropertyValue(this.defaultAdapter, "taskExecutor"))
				.isInstanceOf(ErrorHandlingTaskExecutor.class);
		assertThat(TestUtils.getPropertyValue(this.defaultAdapter, "serializer"))
				.isInstanceOf(JdkSerializationRedisSerializer.class);
		assertThat(TestUtils.getPropertyValue(this.defaultAdapter, "autoStartup", Boolean.class)).isTrue();
		assertThat(TestUtils.getPropertyValue(this.defaultAdapter, "phase")).isEqualTo(Integer.MAX_VALUE / 2);
		assertThat(TestUtils.getPropertyValue(this.defaultAdapter, "outputChannel"))
				.isSameAs(this.defaultAdapterChannel);
		assertThat(TestUtils.getPropertyValue(this.defaultAdapter, "rightPop", Boolean.class)).isTrue();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testInt3017CustomConfig() {
		BoundListOperations<String, byte[]> boundListOperations =
				TestUtils.getPropertyValue(this.customAdapter, "boundListOperations", BoundListOperations.class);
		assertThat(boundListOperations.getKey()).isEqualTo("si.test.Int3017.Inbound2");
		assertThat(TestUtils.getPropertyValue(this.customAdapter, "expectMessage", Boolean.class)).isTrue();
		assertThat(TestUtils.getPropertyValue(this.customAdapter, "receiveTimeout")).isEqualTo(2000L);
		assertThat(TestUtils.getPropertyValue(this.customAdapter, "recoveryInterval")).isEqualTo(3000L);
		assertThat(TestUtils.getPropertyValue(this.customAdapter, "errorChannel")).isSameAs(this.errorChannel);
		assertThat(TestUtils.getPropertyValue(this.customAdapter, "taskExecutor")).isSameAs(this.taskExecutor);
		assertThat(TestUtils.getPropertyValue(this.customAdapter, "serializer")).isSameAs(this.serializer);
		assertThat(TestUtils.getPropertyValue(this.customAdapter, "autoStartup", Boolean.class)).isFalse();
		assertThat(TestUtils.getPropertyValue(this.customAdapter, "phase")).isEqualTo(100);
		assertThat(TestUtils.getPropertyValue(this.customAdapter, "outputChannel")).isSameAs(this.sendChannel);
		assertThat(TestUtils.getPropertyValue(this.customAdapter, "rightPop", Boolean.class)).isFalse();
	}

	@Test
	public void testInt4341ZeroReceiveTimeoutConfig() {
		assertThat(TestUtils.getPropertyValue(this.zeroReceiveTimeoutAdapter, "receiveTimeout")).isEqualTo(0L);
	}

}
