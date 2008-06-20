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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.message.StringMessage;
import org.springframework.security.context.SecurityContext;
import org.springframework.security.context.SecurityContextHolder;
import org.springframework.security.context.SecurityContextImpl;
import org.springframework.security.providers.UsernamePasswordAuthenticationToken;

/**
 * @author Jonas Partner
 */
public class SecureChannelsParserTests {

	private ClassPathXmlApplicationContext applicationContext;

	@Autowired
	@Qualifier("propagationDefault")
	MessageChannel propagationDefault;

	@Autowired
	@Qualifier("excludedFromPropagation")
	MessageChannel excludedFromPropagation;


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
		assertTrue("security context did not propagate by setting message bus level default",
				channelPropagatesSecurityContext(propagationDefault));
	}

	@Test
	public void testNoPropagationOnExcludedChannel() {
		loadApplicationContext(this.getClass().getSimpleName() + "-propagateByDefaultContext.xml");
		assertFalse("security context propagated when channel was explicitly excluded",
				channelPropagatesSecurityContext(excludedFromPropagation));
	}

	@Test
	public void testNoPropagationWithNoDefaultPropagation() {
		loadApplicationContext(this.getClass().getSimpleName() + "-noPropagationByDefaultContext.xml");
		assertFalse("security context propagated when channel default was false and no secured tag present",
				channelPropagatesSecurityContext(propagationDefault));
	}


	private boolean channelPropagatesSecurityContext(MessageChannel channel) {
		login("bob", "bobspassword");
		channel.send(new StringMessage("testMessage"));
		SecurityContext context = (SecurityContext)
				channel.receive(-1).getHeader().getAttribute("SPRING_SECURITY_CONTEXT");
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
