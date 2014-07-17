/*
 * Copyright 2013 the original author or authors.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.integration.redis.inbound.RedisQueueMessageDrivenEndpoint;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.integration.util.ErrorHandlingTaskExecutor;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Artem Bilan
 * @since 3.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
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
	public void testInt3017DefaultConfig() {
		assertSame(this.connectionFactory,
				TestUtils.getPropertyValue(this.defaultAdapter, "boundListOperations.ops.template.connectionFactory"));
		assertEquals("si.test.Int3017.Inbound1", TestUtils.getPropertyValue(this.defaultAdapter, "boundListOperations.key"));
		assertFalse(TestUtils.getPropertyValue(this.defaultAdapter, "expectMessage", Boolean.class));
		assertEquals(1000L, TestUtils.getPropertyValue(this.defaultAdapter, "receiveTimeout"));
		assertEquals(5000L, TestUtils.getPropertyValue(this.defaultAdapter, "recoveryInterval"));
		assertNull(TestUtils.getPropertyValue(this.defaultAdapter, "errorChannel"));
		assertThat(TestUtils.getPropertyValue(this.defaultAdapter, "taskExecutor"),
				Matchers.instanceOf(ErrorHandlingTaskExecutor.class));
		assertThat(TestUtils.getPropertyValue(this.defaultAdapter, "serializer"),
				Matchers.instanceOf(JdkSerializationRedisSerializer.class));
		assertTrue(TestUtils.getPropertyValue(this.defaultAdapter, "autoStartup", Boolean.class));
		assertEquals(Integer.MAX_VALUE / 2, TestUtils.getPropertyValue(this.defaultAdapter, "phase"));
		assertSame(this.defaultAdapterChannel, TestUtils.getPropertyValue(this.defaultAdapter, "outputChannel"));
	}

	@Test
	public void testInt3017CustomConfig() {
		assertSame(this.customRedisConnectionFactory,
				TestUtils.getPropertyValue(this.customAdapter, "boundListOperations.ops.template.connectionFactory"));
		assertEquals("si.test.Int3017.Inbound2", TestUtils.getPropertyValue(this.customAdapter, "boundListOperations.key"));
		assertTrue(TestUtils.getPropertyValue(this.customAdapter, "expectMessage", Boolean.class));
		assertEquals(2000L, TestUtils.getPropertyValue(this.customAdapter, "receiveTimeout"));
		assertEquals(3000L, TestUtils.getPropertyValue(this.customAdapter, "recoveryInterval"));
		assertSame(this.errorChannel, TestUtils.getPropertyValue(this.customAdapter, "errorChannel"));
		assertSame(this.taskExecutor, TestUtils.getPropertyValue(this.customAdapter, "taskExecutor"));
		assertSame(this.serializer, TestUtils.getPropertyValue(this.customAdapter, "serializer"));
		assertFalse(TestUtils.getPropertyValue(this.customAdapter, "autoStartup", Boolean.class));
		assertEquals(100, TestUtils.getPropertyValue(this.customAdapter, "phase"));
		assertSame(this.sendChannel, TestUtils.getPropertyValue(this.customAdapter, "outputChannel"));
	}

}
