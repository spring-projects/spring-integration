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

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptorAdapter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @since 2.1
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class AmqpChannelParserTests {

	@Autowired
	private ApplicationContext context;

	@Test
	public void interceptor() {
		MessageChannel channel = context.getBean("channelWithInterceptor", MessageChannel.class);
		List<?> interceptorList = TestUtils.getPropertyValue(channel, "interceptors.interceptors", List.class);
		assertEquals(1, interceptorList.size());
		assertEquals(TestInterceptor.class, interceptorList.get(0).getClass());
		assertEquals(Integer.MAX_VALUE, TestUtils.getPropertyValue(
				TestUtils.getPropertyValue(channel, "dispatcher"), "maxSubscribers", Integer.class).intValue());
		channel = context.getBean("pubSub", MessageChannel.class);
		Object mbf = context.getBean(IntegrationUtils.INTEGRATION_MESSAGE_BUILDER_FACTORY_BEAN_NAME);
		assertSame(mbf, TestUtils.getPropertyValue(channel, "dispatcher.messageBuilderFactory"));
		assertSame(mbf, TestUtils.getPropertyValue(channel, "container.messageListener.messageBuilderFactory"));
		assertTrue(TestUtils.getPropertyValue(channel, "container.missingQueuesFatal", Boolean.class));
	}

	@Test
	public void subscriberLimit() {
		MessageChannel channel = context.getBean("channelWithSubscriberLimit", MessageChannel.class);
		assertEquals(1, TestUtils.getPropertyValue(
				TestUtils.getPropertyValue(channel, "dispatcher"), "maxSubscribers", Integer.class).intValue());
		assertFalse(TestUtils.getPropertyValue(channel, "container.missingQueuesFatal", Boolean.class));
	}


	private static class TestInterceptor extends ChannelInterceptorAdapter {
	}

}
