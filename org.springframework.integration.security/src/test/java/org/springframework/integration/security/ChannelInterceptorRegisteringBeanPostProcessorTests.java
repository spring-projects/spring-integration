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

package org.springframework.integration.security;

import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;

import org.easymock.EasyMock;
import org.junit.Before;
import org.junit.Test;

import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.channel.ChannelInterceptor;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.selector.MessageSelector;

/**
 * @author Jonas Partner
 */
public class ChannelInterceptorRegisteringBeanPostProcessorTests {

	public ArrayList<String> matchAll;

	@Before
	public void setUp() {
		matchAll = new ArrayList<String>();
		matchAll.add(".*");

	}

	@Test
	public void testWithAbstractMessageChannel() {
		ChannelInterceptorRegisteringBeanPostProcessor postprocessor = new ChannelInterceptorRegisteringBeanPostProcessor(
				new TestInterceptor(), matchAll);
		TestChannel channel = new TestChannel();
		postprocessor.postProcessAfterInitialization(channel, "shouldNotMatter");
		assertNotNull("No channel interceptor present after post processing", channel.channelInterceptor);
	}

	@Test
	public void testWithAbstractMessageChannelAndPatternThatDoes() {
		ChannelInterceptorRegisteringBeanPostProcessor postprocessor = new ChannelInterceptorRegisteringBeanPostProcessor(
				new TestInterceptor(), matchAll);
		TestChannel channel = new TestChannel();
		postprocessor.postProcessAfterInitialization(channel, "shouldNotMatter");
		assertNotNull("No channel interceptor present after post processing", channel.channelInterceptor);
	}

	@Test
	public void testWithMockMessageChanne() {
		MessageChannel channel = EasyMock.createStrictMock(MessageChannel.class);
		EasyMock.replay(channel);
		ChannelInterceptorRegisteringBeanPostProcessor postprocessor = new ChannelInterceptorRegisteringBeanPostProcessor(
				new TestInterceptor(), matchAll);
		postprocessor.postProcessAfterInitialization(channel, "shouldNotMatter");
		EasyMock.verify(channel);
	}


	static class TestInterceptor implements ChannelInterceptor {

		public void postReceive(Message<?> message, MessageChannel channel) {
		}

		public void postSend(Message<?> message, MessageChannel channel, boolean sent) {
		}

		public boolean preReceive(MessageChannel channel) {
			return false;
		}

		public boolean preSend(Message<?> message, MessageChannel channel) {
			return false;
		}
	}


	static class TestChannel extends AbstractMessageChannel {

		ChannelInterceptor channelInterceptor;


		@Override
		public void addInterceptor(ChannelInterceptor interceptor) {
			channelInterceptor = interceptor;
			super.addInterceptor(interceptor);
		}

		@Override
		protected Message<?> doReceive(long timeout) {
			return null;
		}

		@Override
		protected boolean doSend(Message<?> message, long timeout) {
			return false;
		}

		public List<Message<?>> clear() {
			return null;
		}

		public List<Message<?>> purge(MessageSelector selector) {
			return null;
		}

	}

}
