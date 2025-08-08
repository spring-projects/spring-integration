/*
 * Copyright © 2023 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2023-present the original author or authors.
 */

package org.springframework.integration.channel.interceptor;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.core.task.SyncTaskExecutor;
import org.springframework.integration.channel.ExecutorChannel;
import org.springframework.integration.util.ErrorHandlingTaskExecutor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

/**
 * @author Artem Bilan
 *
 * @since 6.2
 */
public class ThreadStatePropagationChannelInterceptorTests {

	@Test
	void ThreadStatePropagationChannelInterceptorsCanBeStacked() {
		TestContext1 ctx1 = new TestContext1();
		TestContext2 ctx2 = new TestContext2();

		List<Object> propagatedContexts = new ArrayList<>();

		var interceptor1 = new ThreadStatePropagationChannelInterceptor<TestContext1>() {

			@Override
			protected TestContext1 obtainPropagatingContext(Message<?> message, MessageChannel channel) {
				return ctx1;
			}

			@Override
			protected void populatePropagatedContext(TestContext1 state, Message<?> message, MessageChannel channel) {
				propagatedContexts.add(state);
			}

		};

		var interceptor2 = new ThreadStatePropagationChannelInterceptor<TestContext2>() {

			@Override
			protected TestContext2 obtainPropagatingContext(Message<?> message, MessageChannel channel) {
				return ctx2;
			}

			@Override
			protected void populatePropagatedContext(TestContext2 state, Message<?> message, MessageChannel channel) {
				propagatedContexts.add(state);
			}

		};

		ExecutorChannel testChannel = new ExecutorChannel(
				new ErrorHandlingTaskExecutor(new SyncTaskExecutor(), ReflectionUtils::rethrowRuntimeException));
		testChannel.setInterceptors(List.of(interceptor1, interceptor2));
		testChannel.setBeanFactory(mock());
		testChannel.afterPropertiesSet();
		testChannel.subscribe(m -> {
		});

		testChannel.send(new GenericMessage<>("test data"));

		assertThat(propagatedContexts.get(0)).isEqualTo(ctx1);
		assertThat(propagatedContexts.get(1)).isEqualTo(ctx2);
	}

	private record TestContext1() {

	}

	private record TestContext2() {

	}

}
