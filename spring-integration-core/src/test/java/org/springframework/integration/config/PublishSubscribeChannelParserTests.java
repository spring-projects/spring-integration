/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config;

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

import static org.assertj.core.api.Assertions.assertThat;

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
