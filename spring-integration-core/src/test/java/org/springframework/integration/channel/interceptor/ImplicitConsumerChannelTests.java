/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.integration.channel.interceptor;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.channel.ChannelInterceptorAware;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.ChannelInterceptorAdapter;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gary Russell
 * @since 4.0
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class ImplicitConsumerChannelTests {

	@Autowired
	private AbstractMessageChannel bar;

	@Autowired
	private AbstractMessageChannel foo;

	@Autowired
	private AbstractMessageChannel baz;

	@Test
	public void testImplicit() {
		// used to fail to load AC (no channel 'bar')
		List<ChannelInterceptor> barInterceptors = bar.getChannelInterceptors();
		assertEquals(2, barInterceptors.size());
		assertThat(barInterceptors.get(0), anyOf(instanceOf(Interceptor1.class), instanceOf(Interceptor2.class)));
		assertThat(barInterceptors.get(1), anyOf(instanceOf(Interceptor1.class), instanceOf(Interceptor2.class)));
		List<ChannelInterceptor> fooInterceptors = foo.getChannelInterceptors();
		assertEquals(2, fooInterceptors.size());
		assertThat(fooInterceptors.get(0), anyOf(instanceOf(WireTap.class), instanceOf(Interceptor2.class)));
		assertThat(fooInterceptors.get(1), anyOf(instanceOf(WireTap.class), instanceOf(Interceptor2.class)));
		List<ChannelInterceptor> bazInterceptors = baz.getChannelInterceptors();
		assertEquals(2, bazInterceptors.size());
		assertThat(bazInterceptors.get(0), anyOf(instanceOf(WireTap.class), instanceOf(Interceptor1.class)));
		assertThat(bazInterceptors.get(1), anyOf(instanceOf(WireTap.class), instanceOf(Interceptor1.class)));
	}

	public static class Interceptor1 extends ChannelInterceptorAdapter implements VetoCapableInterceptor {

		private MessageChannel channel;

		@Override
		public Message<?> preSend(Message<?> message, MessageChannel channel) {
			return null;
		}

		@Override
		public void postSend(Message<?> message, MessageChannel channel, boolean sent) {
		}

		@Override
		public boolean preReceive(MessageChannel channel) {
			return false;
		}

		@Override
		public Message<?> postReceive(Message<?> message, MessageChannel channel) {
			return null;
		}

		public void setChannel(MessageChannel channel) {
			this.channel = channel;
		}

		public MessageChannel getChannel() {
			return channel;
		}

		@Override
		public boolean shouldIntercept(String beanName, ChannelInterceptorAware channel) {
			return !this.channel.equals(channel);
		}

	}

	public static class Interceptor2 extends ChannelInterceptorAdapter implements VetoCapableInterceptor {

		private MessageChannel channel;

		@Override
		public Message<?> preSend(Message<?> message, MessageChannel channel) {
			return null;
		}

		@Override
		public void postSend(Message<?> message, MessageChannel channel, boolean sent) {
		}

		@Override
		public boolean preReceive(MessageChannel channel) {
			return false;
		}

		@Override
		public Message<?> postReceive(Message<?> message, MessageChannel channel) {
			return null;
		}

		public void setChannel(MessageChannel channel) {
			this.channel = channel;
		}

		public MessageChannel getChannel() {
			return channel;
		}

		@Override
		public boolean shouldIntercept(String beanName, ChannelInterceptorAware channel) {
			return !this.channel.equals(channel);
		}

	}

}
