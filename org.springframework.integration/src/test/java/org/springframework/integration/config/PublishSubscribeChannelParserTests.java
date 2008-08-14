/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.core.task.TaskExecutor;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.dispatcher.BroadcastingDispatcher;

/**
 * @author Mark Fisher
 */
public class PublishSubscribeChannelParserTests {

	@Test
	public void defaultChannel() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"publishSubscribeChannelParserTests.xml", this.getClass());
		PublishSubscribeChannel channel = (PublishSubscribeChannel)
				context.getBean("defaultChannel");
		DirectFieldAccessor accessor = new DirectFieldAccessor(channel);
		BroadcastingDispatcher dispatcher = (BroadcastingDispatcher)
				accessor.getPropertyValue("dispatcher");
		DirectFieldAccessor dispatcherAccessor = new DirectFieldAccessor(dispatcher);
		assertNull(dispatcherAccessor.getPropertyValue("taskExecutor"));
		assertFalse((Boolean) dispatcherAccessor.getPropertyValue("applySequence"));
	}

	@Test
	public void applySequenceEnabled() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"publishSubscribeChannelParserTests.xml", this.getClass());
		PublishSubscribeChannel channel = (PublishSubscribeChannel)
				context.getBean("channelWithApplySequenceEnabled");
		DirectFieldAccessor accessor = new DirectFieldAccessor(channel);
		BroadcastingDispatcher dispatcher = (BroadcastingDispatcher)
				accessor.getPropertyValue("dispatcher");
		assertTrue((Boolean) new DirectFieldAccessor(dispatcher).getPropertyValue("applySequence"));
	}

	@Test
	public void channelWithTaskExecutor() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"publishSubscribeChannelParserTests.xml", this.getClass());
		PublishSubscribeChannel channel = (PublishSubscribeChannel)
				context.getBean("channelWithTaskExecutor");
		DirectFieldAccessor accessor = new DirectFieldAccessor(channel);
		BroadcastingDispatcher dispatcher = (BroadcastingDispatcher)
				accessor.getPropertyValue("dispatcher");
		DirectFieldAccessor dispatcherAccessor = new DirectFieldAccessor(dispatcher);
		TaskExecutor executor = (TaskExecutor) dispatcherAccessor.getPropertyValue("taskExecutor");
		assertNotNull(executor);
		assertEquals(context.getBean("pool"), executor);
	}

}
