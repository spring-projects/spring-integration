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

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.amqp.inbound.AmqpInboundChannelAdapter;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 * @since 2.1
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class AmqpInboundChannelAdapterParserTests {

	@Autowired
	private ApplicationContext context;

	@Test
	public void verifyIdAsChannel() {
		Object channel = context.getBean("rabbitInbound");
		Object adapter = context.getBean("rabbitInbound.adapter");
		assertEquals(DirectChannel.class, channel.getClass());
		assertEquals(AmqpInboundChannelAdapter.class, adapter.getClass());
		assertEquals(Boolean.TRUE, TestUtils.getPropertyValue(adapter, "autoStartup"));
		assertEquals(0, TestUtils.getPropertyValue(adapter, "phase"));
	}

	@Test
	public void verifyLifeCycle() {
		Object adapter = context.getBean("autoStartFalse.adapter");
		assertEquals(Boolean.FALSE, TestUtils.getPropertyValue(adapter, "autoStartup"));
		assertEquals(123, TestUtils.getPropertyValue(adapter, "phase"));
	}
}
