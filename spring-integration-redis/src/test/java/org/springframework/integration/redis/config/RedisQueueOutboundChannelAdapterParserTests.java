/*
 * Copyright 2013-2016 the original author or authors.
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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.hamcrest.Matchers;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.aop.framework.Advised;
import org.springframework.aop.support.AopUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.expression.Expression;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.handler.advice.RequestHandlerRetryAdvice;
import org.springframework.integration.redis.outbound.RedisQueueOutboundChannelAdapter;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Artem Bilan
 * @author Gary Russell
 * @author Rainer Frey
 * @since 3.0
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class RedisQueueOutboundChannelAdapterParserTests {

	@Autowired
	@Qualifier("redisConnectionFactory")
	private RedisConnectionFactory connectionFactory;

	@Autowired
	@Qualifier("customRedisConnectionFactory")
	private RedisConnectionFactory customRedisConnectionFactory;

	@Autowired
	@Qualifier("defaultAdapter")
	private EventDrivenConsumer defaultEndpoint;

	@Autowired
	@Qualifier("defaultAdapter.handler")
	private RedisQueueOutboundChannelAdapter defaultAdapter;

	@Autowired
	@Qualifier("customAdapter.handler")
	private RedisQueueOutboundChannelAdapter customAdapter;

	@Autowired
	private RedisSerializer<?> serializer;

	@Test
	public void testInt3017DefaultConfig() throws Exception {
		assertSame(this.connectionFactory, TestUtils.getPropertyValue(this.defaultAdapter, "template.connectionFactory"));
		assertEquals("foo", TestUtils.getPropertyValue(this.defaultAdapter, "queueNameExpression", Expression.class).getExpressionString());
		assertTrue(TestUtils.getPropertyValue(this.defaultAdapter, "extractPayload", Boolean.class));
		assertFalse(TestUtils.getPropertyValue(this.defaultAdapter, "serializerExplicitlySet", Boolean.class));

		Object handler = TestUtils.getPropertyValue(this.defaultEndpoint, "handler");

		assertTrue(AopUtils.isAopProxy(handler));

		assertSame(((Advised) handler).getTargetSource().getTarget(), this.defaultAdapter);

		assertThat(TestUtils.getPropertyValue(handler, "h.advised.advisors.first.item.advice"),
				Matchers.instanceOf(RequestHandlerRetryAdvice.class));
		assertTrue(TestUtils.getPropertyValue(this.defaultAdapter, "leftPush", Boolean.class));
	}

	@Test
	public void testInt3017CustomConfig() {
		assertSame(this.customRedisConnectionFactory, TestUtils.getPropertyValue(this.customAdapter, "template.connectionFactory"));
		assertEquals("headers['redis_queue']", TestUtils.getPropertyValue(this.customAdapter, "queueNameExpression", Expression.class).getExpressionString());
		assertFalse(TestUtils.getPropertyValue(this.customAdapter, "extractPayload", Boolean.class));
		assertTrue(TestUtils.getPropertyValue(this.customAdapter, "serializerExplicitlySet", Boolean.class));
		assertSame(this.serializer, TestUtils.getPropertyValue(this.customAdapter, "serializer"));
		assertFalse(TestUtils.getPropertyValue(this.customAdapter, "leftPush", Boolean.class));
	}

}
