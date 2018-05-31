/*
 * Copyright 2002-2018 the original author or authors.
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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.Executor;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.dispatcher.BroadcastingDispatcher;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.integration.util.ErrorHandlingTaskExecutor;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.util.ErrorHandler;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 */
@RunWith(SpringRunner.class)
public class PublishSubscribeChannelParserTests {

	@Autowired
	private ApplicationContext context;

	@Test
	public void defaultChannel() {
		PublishSubscribeChannel channel = this.context.getBean("defaultChannel", PublishSubscribeChannel.class);
		DirectFieldAccessor accessor = new DirectFieldAccessor(channel);
		BroadcastingDispatcher dispatcher = (BroadcastingDispatcher)
				accessor.getPropertyValue("dispatcher");
		dispatcher.setApplySequence(true);
		dispatcher.addHandler(message -> { });
		dispatcher.dispatch(new GenericMessage<>("foo"));
		DirectFieldAccessor dispatcherAccessor = new DirectFieldAccessor(dispatcher);
		assertNull(dispatcherAccessor.getPropertyValue("executor"));
		assertFalse((Boolean) dispatcherAccessor.getPropertyValue("ignoreFailures"));
		assertTrue((Boolean) dispatcherAccessor.getPropertyValue("applySequence"));
		Object mbf = this.context.getBean(IntegrationUtils.INTEGRATION_MESSAGE_BUILDER_FACTORY_BEAN_NAME);
		assertSame(mbf, dispatcherAccessor.getPropertyValue("messageBuilderFactory"));
	}

	@Test
	public void ignoreFailures() {
		PublishSubscribeChannel channel =
				this.context.getBean("channelWithIgnoreFailures", PublishSubscribeChannel.class);
		DirectFieldAccessor accessor = new DirectFieldAccessor(channel);
		BroadcastingDispatcher dispatcher = (BroadcastingDispatcher)
				accessor.getPropertyValue("dispatcher");
		assertTrue((Boolean) new DirectFieldAccessor(dispatcher).getPropertyValue("ignoreFailures"));
	}

	@Test
	public void applySequenceEnabled() {
		PublishSubscribeChannel channel =
				this.context.getBean("channelWithApplySequenceEnabled", PublishSubscribeChannel.class);
		DirectFieldAccessor accessor = new DirectFieldAccessor(channel);
		BroadcastingDispatcher dispatcher = (BroadcastingDispatcher)
				accessor.getPropertyValue("dispatcher");
		assertTrue((Boolean) new DirectFieldAccessor(dispatcher).getPropertyValue("applySequence"));
	}

	@Test
	public void channelWithTaskExecutor() {
		PublishSubscribeChannel channel =
				this.context.getBean("channelWithTaskExecutor", PublishSubscribeChannel.class);
		DirectFieldAccessor accessor = new DirectFieldAccessor(channel);
		BroadcastingDispatcher dispatcher = (BroadcastingDispatcher)
				accessor.getPropertyValue("dispatcher");
		DirectFieldAccessor dispatcherAccessor = new DirectFieldAccessor(dispatcher);
		Executor executor = (Executor) dispatcherAccessor.getPropertyValue("executor");
		assertNotNull(executor);
		assertEquals(ErrorHandlingTaskExecutor.class, executor.getClass());
		DirectFieldAccessor executorAccessor = new DirectFieldAccessor(executor);
		Executor innerExecutor = (Executor) executorAccessor.getPropertyValue("executor");
		assertEquals(context.getBean("pool"), innerExecutor);
	}

	@Test
	public void ignoreFailuresWithTaskExecutor() {
		PublishSubscribeChannel channel =
				this.context.getBean("channelWithIgnoreFailuresAndTaskExecutor", PublishSubscribeChannel.class);
		DirectFieldAccessor accessor = new DirectFieldAccessor(channel);
		BroadcastingDispatcher dispatcher = (BroadcastingDispatcher)
				accessor.getPropertyValue("dispatcher");
		DirectFieldAccessor dispatcherAccessor = new DirectFieldAccessor(dispatcher);
		assertTrue((Boolean) dispatcherAccessor.getPropertyValue("ignoreFailures"));
		Executor executor = (Executor) dispatcherAccessor.getPropertyValue("executor");
		assertNotNull(executor);
		assertEquals(ErrorHandlingTaskExecutor.class, executor.getClass());
		DirectFieldAccessor executorAccessor = new DirectFieldAccessor(executor);
		Executor innerExecutor = (Executor) executorAccessor.getPropertyValue("executor");
		assertEquals(this.context.getBean("pool"), innerExecutor);
	}

	@Test
	public void applySequenceEnabledWithTaskExecutor() {
		PublishSubscribeChannel channel =
				this.context.getBean("channelWithApplySequenceEnabledAndTaskExecutor", PublishSubscribeChannel.class);
		DirectFieldAccessor accessor = new DirectFieldAccessor(channel);
		BroadcastingDispatcher dispatcher = (BroadcastingDispatcher)
				accessor.getPropertyValue("dispatcher");
		DirectFieldAccessor dispatcherAccessor = new DirectFieldAccessor(dispatcher);
		assertTrue((Boolean) dispatcherAccessor.getPropertyValue("applySequence"));
		Executor executor = (Executor) dispatcherAccessor.getPropertyValue("executor");
		assertNotNull(executor);
		assertEquals(ErrorHandlingTaskExecutor.class, executor.getClass());
		DirectFieldAccessor executorAccessor = new DirectFieldAccessor(executor);
		Executor innerExecutor = (Executor) executorAccessor.getPropertyValue("executor");
		assertEquals(this.context.getBean("pool"), innerExecutor);
	}

	@Test
	public void channelWithErrorHandler() {
		PublishSubscribeChannel channel =
				this.context.getBean("channelWithErrorHandler", PublishSubscribeChannel.class);
		DirectFieldAccessor accessor = new DirectFieldAccessor(channel);
		ErrorHandler errorHandler = (ErrorHandler) accessor.getPropertyValue("errorHandler");
		assertNotNull(errorHandler);
		assertEquals(this.context.getBean("testErrorHandler"), errorHandler);
	}

}
