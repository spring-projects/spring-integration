/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.config;

import java.util.concurrent.Executor;

import org.junit.jupiter.api.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.dispatcher.BroadcastingDispatcher;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.integration.util.ErrorHandlingTaskExecutor;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.util.ErrorHandler;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 */
@SpringJUnitConfig
@DirtiesContext
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
		dispatcher.addHandler(message -> {
		});
		dispatcher.dispatch(new GenericMessage<>("foo"));
		DirectFieldAccessor dispatcherAccessor = new DirectFieldAccessor(dispatcher);
		assertThat(dispatcherAccessor.getPropertyValue("executor")).isNull();
		assertThat((Boolean) dispatcherAccessor.getPropertyValue("ignoreFailures")).isFalse();
		assertThat((Boolean) dispatcherAccessor.getPropertyValue("applySequence")).isTrue();
		Object mbf = this.context.getBean(IntegrationUtils.INTEGRATION_MESSAGE_BUILDER_FACTORY_BEAN_NAME);
		assertThat(dispatcherAccessor.getPropertyValue("messageBuilderFactory")).isSameAs(mbf);
	}

	@Test
	public void ignoreFailures() {
		PublishSubscribeChannel channel =
				this.context.getBean("channelWithIgnoreFailures", PublishSubscribeChannel.class);
		DirectFieldAccessor accessor = new DirectFieldAccessor(channel);
		BroadcastingDispatcher dispatcher = (BroadcastingDispatcher)
				accessor.getPropertyValue("dispatcher");
		assertThat((Boolean) new DirectFieldAccessor(dispatcher).getPropertyValue("ignoreFailures")).isTrue();
	}

	@Test
	public void applySequenceEnabled() {
		PublishSubscribeChannel channel =
				this.context.getBean("channelWithApplySequenceEnabled", PublishSubscribeChannel.class);
		DirectFieldAccessor accessor = new DirectFieldAccessor(channel);
		BroadcastingDispatcher dispatcher = (BroadcastingDispatcher)
				accessor.getPropertyValue("dispatcher");
		assertThat((Boolean) new DirectFieldAccessor(dispatcher).getPropertyValue("applySequence")).isTrue();
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
		assertThat(executor).isNotNull();
		assertThat(executor.getClass()).isEqualTo(ErrorHandlingTaskExecutor.class);
		DirectFieldAccessor executorAccessor = new DirectFieldAccessor(executor);
		Executor innerExecutor = (Executor) executorAccessor.getPropertyValue("executor");
		assertThat(innerExecutor).isEqualTo(context.getBean("pool"));
	}

	@Test
	public void ignoreFailuresWithTaskExecutor() {
		PublishSubscribeChannel channel =
				this.context.getBean("channelWithIgnoreFailuresAndTaskExecutor", PublishSubscribeChannel.class);
		DirectFieldAccessor accessor = new DirectFieldAccessor(channel);
		BroadcastingDispatcher dispatcher = (BroadcastingDispatcher)
				accessor.getPropertyValue("dispatcher");
		DirectFieldAccessor dispatcherAccessor = new DirectFieldAccessor(dispatcher);
		assertThat((Boolean) dispatcherAccessor.getPropertyValue("ignoreFailures")).isTrue();
		Executor executor = (Executor) dispatcherAccessor.getPropertyValue("executor");
		assertThat(executor).isNotNull();
		assertThat(executor.getClass()).isEqualTo(ErrorHandlingTaskExecutor.class);
		DirectFieldAccessor executorAccessor = new DirectFieldAccessor(executor);
		Executor innerExecutor = (Executor) executorAccessor.getPropertyValue("executor");
		assertThat(innerExecutor).isEqualTo(this.context.getBean("pool"));
	}

	@Test
	public void applySequenceEnabledWithTaskExecutor() {
		PublishSubscribeChannel channel =
				this.context.getBean("channelWithApplySequenceEnabledAndTaskExecutor", PublishSubscribeChannel.class);
		DirectFieldAccessor accessor = new DirectFieldAccessor(channel);
		BroadcastingDispatcher dispatcher = (BroadcastingDispatcher)
				accessor.getPropertyValue("dispatcher");
		DirectFieldAccessor dispatcherAccessor = new DirectFieldAccessor(dispatcher);
		assertThat((Boolean) dispatcherAccessor.getPropertyValue("applySequence")).isTrue();
		Executor executor = (Executor) dispatcherAccessor.getPropertyValue("executor");
		assertThat(executor).isNotNull();
		assertThat(executor.getClass()).isEqualTo(ErrorHandlingTaskExecutor.class);
		DirectFieldAccessor executorAccessor = new DirectFieldAccessor(executor);
		Executor innerExecutor = (Executor) executorAccessor.getPropertyValue("executor");
		assertThat(innerExecutor).isEqualTo(this.context.getBean("pool"));
	}

	@Test
	public void channelWithErrorHandler() {
		PublishSubscribeChannel channel =
				this.context.getBean("channelWithErrorHandler", PublishSubscribeChannel.class);
		DirectFieldAccessor accessor = new DirectFieldAccessor(channel);
		ErrorHandler errorHandler = (ErrorHandler) accessor.getPropertyValue("errorHandler");
		assertThat(errorHandler).isNotNull();
		assertThat(errorHandler).isEqualTo(this.context.getBean("testErrorHandler"));
	}

}
