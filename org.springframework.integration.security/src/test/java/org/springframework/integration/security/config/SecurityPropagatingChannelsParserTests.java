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

import org.junit.After;
import org.junit.Test;

import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.PollableChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.message.StringMessage;
import org.springframework.security.context.SecurityContext;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.context.SecurityContextImpl;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;

/**
 * @author Jonas Partner
 */
public class SecurityPropagatingChannelsParserTests {

	private ClassPathXmlApplicationContext applicationContext;

	@After
	public void tearDown() {
		if (applicationContext != null) {
			applicationContext.close();
		}
		SecurityContextHolder.clearContext();
	}

	@Test
	public void testPropagationByDefault() {
		loadApplicationContext(this.getClass().getSimpleName() + "-propagateByDefaultContext.xml");
		QueueChannel channel = new QueueChannel();
		applicationContext.getAutowireCapableBeanFactory().applyBeanPostProcessorsAfterInitialization(channel,
				"Does not matter");
		assertTrue("security context did not propagate by setting message bus level default",
				channelPropagatesSecurityContext(channel));
	}

	// @Test
	// public void testNoPropagationOnExcludedChannel() {
	// loadApplicationContext(this.getClass().getSimpleName() +
	// "-propagateByDefaultContext.xml");
	// assertFalse("security context propagated when channel was explicitly
	// excluded",
	// channelPropagatesSecurityContext(excludedFromPropagation));
	// }
	//
	@Test
	public void testNoPropagationWithExcludedChannel() {
		loadApplicationContext(this.getClass().getSimpleName() + "-noPropagationByDefaultContext.xml");
		QueueChannel channel = new QueueChannel();
		applicationContext.getAutowireCapableBeanFactory().applyBeanPostProcessorsAfterInitialization(channel,
				"adminSpecial");
		assertFalse("security context propagated when channel excluded", channelPropagatesSecurityContext(channel));
	}

	private boolean channelPropagatesSecurityContext(PollableChannel channel) {
		login("bob", "bobspassword");
		channel.send(new StringMessage("testMessage"));
		SecurityContext context = (SecurityContext) channel.receive(-1).getHeaders().get(
				"SPRING_SECURITY_CONTEXT");
		return context != null;
	}

	private void login(String username, String password) {
		UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(username, password);
		SecurityContext context = new SecurityContextImpl();
		context.setAuthentication(authToken);
		SecurityContextHolder.setContext(context);
	}

	private void loadApplicationContext(String resource) {
		this.applicationContext = new ClassPathXmlApplicationContext(resource, this.getClass());
		AutowireCapableBeanFactory beanFactory = this.applicationContext.getAutowireCapableBeanFactory();
		beanFactory.autowireBean(this);
	}

}
