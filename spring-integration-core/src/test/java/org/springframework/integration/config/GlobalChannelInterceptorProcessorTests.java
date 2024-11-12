/*
 * Copyright 2017-2024 the original author or authors.
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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.integration.channel.interceptor.GlobalChannelInterceptorWrapper;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.InterceptableChannel;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @author Meherzad Lahewala
 *
 * @since 5.0
 */
public class GlobalChannelInterceptorProcessorTests {

	private GlobalChannelInterceptorProcessor globalChannelInterceptorProcessor;

	private ListableBeanFactory beanFactory;

	@BeforeEach
	public void setup() {
		this.globalChannelInterceptorProcessor = new GlobalChannelInterceptorProcessor();
		this.beanFactory = mock(ListableBeanFactory.class);
		this.globalChannelInterceptorProcessor.setBeanFactory(this.beanFactory);
	}

	@Test
	public void testProcessorWithNoInterceptor() {
		when(this.beanFactory.getBeansOfType(GlobalChannelInterceptorWrapper.class))
				.thenReturn(Collections.emptyMap());
		this.globalChannelInterceptorProcessor.afterSingletonsInstantiated();
		verify(this.beanFactory)
				.getBeansOfType(GlobalChannelInterceptorWrapper.class);
		verify(this.beanFactory, never())
				.getBeansOfType(InterceptableChannel.class);
	}

	@Test
	public void testProcessorWithInterceptorDefaultPattern() {
		Map<String, GlobalChannelInterceptorWrapper> interceptors = new HashMap<>();
		Map<String, InterceptableChannel> channels = new HashMap<>();
		ChannelInterceptor channelInterceptor = mock();
		GlobalChannelInterceptorWrapper globalChannelInterceptorWrapper =
				new GlobalChannelInterceptorWrapper(channelInterceptor);

		InterceptableChannel channel = mock();

		interceptors.put("Test-1", globalChannelInterceptorWrapper);
		channels.put("Test-1", channel);
		when(this.beanFactory.getBeansOfType(GlobalChannelInterceptorWrapper.class))
				.thenReturn(interceptors);
		when(this.beanFactory.getBeansOfType(InterceptableChannel.class))
				.thenReturn(channels);

		this.globalChannelInterceptorProcessor.afterSingletonsInstantiated();

		verify(channel)
				.addInterceptor(channelInterceptor);
	}

	@Test
	public void testProcessorWithInterceptorMatchingPattern() {
		Map<String, GlobalChannelInterceptorWrapper> interceptors = new HashMap<>();
		Map<String, InterceptableChannel> channels = new HashMap<>();
		ChannelInterceptor channelInterceptor = mock();
		GlobalChannelInterceptorWrapper globalChannelInterceptorWrapper =
				new GlobalChannelInterceptorWrapper(channelInterceptor);

		InterceptableChannel channel = mock();

		globalChannelInterceptorWrapper.setPatterns(new String[] {"Te*"});
		interceptors.put("Test-1", globalChannelInterceptorWrapper);
		channels.put("Test-1", channel);
		when(this.beanFactory.getBeansOfType(GlobalChannelInterceptorWrapper.class))
				.thenReturn(interceptors);
		when(this.beanFactory.getBeansOfType(InterceptableChannel.class))
				.thenReturn(channels);
		this.globalChannelInterceptorProcessor.afterSingletonsInstantiated();

		verify(channel)
				.addInterceptor(channelInterceptor);
	}

	@Test
	public void testProcessorWithInterceptorNotMatchingPattern() {
		Map<String, GlobalChannelInterceptorWrapper> interceptors = new HashMap<>();
		Map<String, InterceptableChannel> channels = new HashMap<>();
		ChannelInterceptor channelInterceptor = mock();
		GlobalChannelInterceptorWrapper globalChannelInterceptorWrapper =
				new GlobalChannelInterceptorWrapper(channelInterceptor);

		InterceptableChannel channel = mock();

		globalChannelInterceptorWrapper.setPatterns(new String[] {"te*"});
		interceptors.put("Test-1", globalChannelInterceptorWrapper);
		channels.put("Test-1", channel);
		when(this.beanFactory.getBeansOfType(GlobalChannelInterceptorWrapper.class))
				.thenReturn(interceptors);
		when(this.beanFactory.getBeansOfType(InterceptableChannel.class))
				.thenReturn(channels);

		this.globalChannelInterceptorProcessor.afterSingletonsInstantiated();

		verify(channel, never())
				.addInterceptor(channelInterceptor);
	}

	@Test
	public void testProcessorWithInterceptorMatchingNegativePattern() {
		Map<String, GlobalChannelInterceptorWrapper> interceptors = new HashMap<>();
		Map<String, InterceptableChannel> channels = new HashMap<>();
		ChannelInterceptor channelInterceptor = mock();
		GlobalChannelInterceptorWrapper globalChannelInterceptorWrapper =
				new GlobalChannelInterceptorWrapper(channelInterceptor);

		InterceptableChannel channel = mock();

		globalChannelInterceptorWrapper.setPatterns(new String[] {"!te*", "!Te*"});
		interceptors.put("Test-1", globalChannelInterceptorWrapper);
		channels.put("Test-1", channel);
		when(this.beanFactory.getBeansOfType(GlobalChannelInterceptorWrapper.class))
				.thenReturn(interceptors);
		when(this.beanFactory.getBeansOfType(InterceptableChannel.class))
				.thenReturn(channels);
		this.globalChannelInterceptorProcessor.afterSingletonsInstantiated();

		verify(channel, never())
				.addInterceptor(channelInterceptor);
	}

}
