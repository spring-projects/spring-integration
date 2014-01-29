/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.integration.jms.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.channel.ChannelInterceptorAware;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.ChannelInterceptorAdapter;

/**
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Artem Bilan
 *
 * @since 2.0.1
 */
public class GlobalChannelInterceptorTests {

	@Test
	public void testJmsChannel() {
		ActiveMqTestUtils.prepare();
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"GlobalChannelInterceptorTests-context.xml",  GlobalChannelInterceptorTests.class);
		ChannelInterceptorAware jmsChannel = context.getBean("jmsChannel", AbstractMessageChannel.class);
		List<ChannelInterceptor> interceptors = jmsChannel.getChannelInterceptors();
		assertNotNull(interceptors);
		assertEquals(1, interceptors.size());
		assertTrue(interceptors.get(0) instanceof SampleInterceptor);
	}


	public static class SampleInterceptor extends ChannelInterceptorAdapter {
	}

}
