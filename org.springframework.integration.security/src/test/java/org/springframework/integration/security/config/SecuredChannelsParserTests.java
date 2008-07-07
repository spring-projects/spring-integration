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

package org.springframework.integration.security.config;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.channel.ChannelInterceptor;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.selector.MessageSelector;
import org.springframework.integration.security.channel.SecurityEnforcingChannelInterceptor;
import org.springframework.security.SecurityConfig;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.AbstractJUnit4SpringContextTests;

/**
 * @author Jonas Partner
 */
@ContextConfiguration
public class SecuredChannelsParserTests extends AbstractJUnit4SpringContextTests {

	TestMessageChannel messageChannel;

	@Before
	public void setUp() {
		messageChannel = new TestMessageChannel();
	}

	@Test
	public void testAdminRequiredForSend() {
		applicationContext.getAutowireCapableBeanFactory().applyBeanPostProcessorsAfterInitialization(messageChannel,
				"adminRequiredForSend");
		assertEquals("Wrong count of interceptors ", 1, messageChannel.interceptors.size());
		SecurityEnforcingChannelInterceptor interceptor = (SecurityEnforcingChannelInterceptor) messageChannel.interceptors
				.get(0);
		assertTrue("ROLE_ADMIN not found as send attribute", interceptor.getSendSecurityAttributes().contains(
				new SecurityConfig("ROLE_ADMIN")));
		assertNull("Receive security attribute were not null", interceptor.getReceiveSecurityAttributes());
	}

	@Test
	public void testAdminOrUserRequiredForSend() {
		applicationContext.getAutowireCapableBeanFactory().applyBeanPostProcessorsAfterInitialization(messageChannel,
				"adminOrUserRequiredForSend");
		assertEquals("Wrong count of interceptors ", 1, messageChannel.interceptors.size());
		SecurityEnforcingChannelInterceptor interceptor = (SecurityEnforcingChannelInterceptor) messageChannel.interceptors
				.get(0);
		assertTrue("ROLE_ADMIN not found as send attribute", interceptor.getSendSecurityAttributes().contains(
				new SecurityConfig("ROLE_ADMIN")));
		assertTrue("ROLE_USER not found as send attribute", interceptor.getSendSecurityAttributes().contains(
				new SecurityConfig("ROLE_USER")));
		assertNull("Receive security attribute were not null", interceptor.getReceiveSecurityAttributes());
	}

	@Test
	public void testAdminRequiredForReceive() {
		applicationContext.getAutowireCapableBeanFactory().applyBeanPostProcessorsAfterInitialization(messageChannel,
				"adminRequiredForReceive");
		assertEquals("Wrong count of interceptors ", 1, messageChannel.interceptors.size());
		SecurityEnforcingChannelInterceptor interceptor = (SecurityEnforcingChannelInterceptor) messageChannel.interceptors
				.get(0);
		assertTrue("ROLE_ADMIN not found as receive attribute", interceptor.getReceiveSecurityAttributes().contains(
				new SecurityConfig("ROLE_ADMIN")));
		assertNull("Send security attribute were not null", interceptor.getSendSecurityAttributes());
	}

	@Test
	public void testAdminOrUserRequiredForReceive() {
		applicationContext.getAutowireCapableBeanFactory().applyBeanPostProcessorsAfterInitialization(messageChannel,
				"adminOrUserRequiredForReceive");
		assertEquals("Wrong count of interceptors ", 1, messageChannel.interceptors.size());
		SecurityEnforcingChannelInterceptor interceptor = (SecurityEnforcingChannelInterceptor) messageChannel.interceptors
				.get(0);
		assertTrue("ROLE_ADMIN not found as receive attribute", interceptor.getReceiveSecurityAttributes().contains(
				new SecurityConfig("ROLE_ADMIN")));
		assertTrue("ROLE_USER not found as receive attribute", interceptor.getReceiveSecurityAttributes().contains(
				new SecurityConfig("ROLE_USER")));
		assertNull("Send security attribute were not null", interceptor.getSendSecurityAttributes());
	}

	@Test
	public void testAdminRequiredForSendAndReceive() {
		applicationContext.getAutowireCapableBeanFactory().applyBeanPostProcessorsAfterInitialization(messageChannel,
				"adminForSendAndReceive");
		assertEquals("Wrong count of interceptors ", 1, messageChannel.interceptors.size());
		SecurityEnforcingChannelInterceptor interceptor = (SecurityEnforcingChannelInterceptor) messageChannel.interceptors
				.get(0);
		assertTrue("ROLE_ADMIN not found as receive attribute", interceptor.getReceiveSecurityAttributes().contains(
				new SecurityConfig("ROLE_ADMIN")));
		assertTrue("ROLE_USER not found as send attribute", interceptor.getSendSecurityAttributes().contains(
				new SecurityConfig("ROLE_ADMIN")));
	}

	static class TestMessageChannel extends AbstractMessageChannel {

		List<ChannelInterceptor> interceptors = new ArrayList<ChannelInterceptor>();

		public TestMessageChannel() {
			super(null);
		}

		@Override
		protected Message<?> doReceive(long timeout) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		protected boolean doSend(Message<?> message, long timeout) {
			// TODO Auto-generated method stub
			return false;
		}

		public List<Message<?>> clear() {
			// TODO Auto-generated method stub
			return null;
		}

		public List<Message<?>> purge(MessageSelector selector) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void addInterceptor(ChannelInterceptor interceptor) {
			interceptors.add(interceptor);
		}

	}

}
