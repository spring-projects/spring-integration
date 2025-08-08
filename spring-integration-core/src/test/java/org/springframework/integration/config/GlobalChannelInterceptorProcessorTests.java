/*
 * Copyright © 2017 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2017-present the original author or authors.
 */

package org.springframework.integration.config;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.integration.channel.interceptor.GlobalChannelInterceptorWrapper;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.InterceptableChannel;

import static org.mockito.Mockito.mock;
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

	@Before
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
		verify(this.beanFactory, Mockito.never())
				.getBeansOfType(InterceptableChannel.class);
	}

	@Test
	public void testProcessorWithInterceptorDefaultPattern() {
		Map<String, GlobalChannelInterceptorWrapper> interceptors = new HashMap<>();
		Map<String, InterceptableChannel> channels = new HashMap<>();
		ChannelInterceptor channelInterceptor = Mockito.mock(ChannelInterceptor.class);
		GlobalChannelInterceptorWrapper globalChannelInterceptorWrapper =
				new GlobalChannelInterceptorWrapper(channelInterceptor);

		InterceptableChannel channel = Mockito.mock(InterceptableChannel.class);

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
		ChannelInterceptor channelInterceptor = Mockito.mock(ChannelInterceptor.class);
		GlobalChannelInterceptorWrapper globalChannelInterceptorWrapper =
				new GlobalChannelInterceptorWrapper(channelInterceptor);

		InterceptableChannel channel = Mockito.mock(InterceptableChannel.class);

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
		ChannelInterceptor channelInterceptor = Mockito.mock(ChannelInterceptor.class);
		GlobalChannelInterceptorWrapper globalChannelInterceptorWrapper =
				new GlobalChannelInterceptorWrapper(channelInterceptor);

		InterceptableChannel channel = Mockito.mock(InterceptableChannel.class);

		globalChannelInterceptorWrapper.setPatterns(new String[] {"te*"});
		interceptors.put("Test-1", globalChannelInterceptorWrapper);
		channels.put("Test-1", channel);
		when(this.beanFactory.getBeansOfType(GlobalChannelInterceptorWrapper.class))
				.thenReturn(interceptors);
		when(this.beanFactory.getBeansOfType(InterceptableChannel.class))
				.thenReturn(channels);

		this.globalChannelInterceptorProcessor.afterSingletonsInstantiated();

		verify(channel, Mockito.never())
				.addInterceptor(channelInterceptor);
	}

	@Test
	public void testProcessorWithInterceptorMatchingNegativePattern() {
		Map<String, GlobalChannelInterceptorWrapper> interceptors = new HashMap<>();
		Map<String, InterceptableChannel> channels = new HashMap<>();
		ChannelInterceptor channelInterceptor = Mockito.mock(ChannelInterceptor.class);
		GlobalChannelInterceptorWrapper globalChannelInterceptorWrapper =
				new GlobalChannelInterceptorWrapper(channelInterceptor);

		InterceptableChannel channel = Mockito.mock(InterceptableChannel.class);

		globalChannelInterceptorWrapper.setPatterns(new String[] {"!te*", "!Te*"});
		interceptors.put("Test-1", globalChannelInterceptorWrapper);
		channels.put("Test-1", channel);
		when(this.beanFactory.getBeansOfType(GlobalChannelInterceptorWrapper.class))
				.thenReturn(interceptors);
		when(this.beanFactory.getBeansOfType(InterceptableChannel.class))
				.thenReturn(channels);
		this.globalChannelInterceptorProcessor.afterSingletonsInstantiated();

		verify(channel, Mockito.never())
				.addInterceptor(channelInterceptor);
	}

}
