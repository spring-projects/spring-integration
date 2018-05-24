/*
 * Copyright 2002-2018 the original author or authors.
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

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.Ordered;
import org.springframework.integration.channel.ChannelInterceptorAware;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Oleg Zhurakousky
 * @author David Turanski
 * @author Artem Bilan
 * @author Meherzad Lahewala
 *
 * @since 2.0
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class GlobalChannelInterceptorTests {

	@Autowired
	ConfigurableApplicationContext applicationContext;

	@Autowired
	@Qualifier("inputC")
	ChannelInterceptorAware inputCChannel;


	@Test
	public void validateGlobalInterceptor() throws Exception {
		Map<String, ChannelInterceptorAware> channels = applicationContext.getBeansOfType(ChannelInterceptorAware.class);
		for (String channelName : channels.keySet()) {
			ChannelInterceptorAware channel = channels.get(channelName);
			if (channelName.equals("nullChannel")) {
				continue;
			}

			ChannelInterceptor[] interceptors = channel.getChannelInterceptors()
					.toArray(new ChannelInterceptor[channel.getChannelInterceptors().size()]);
			if (channelName.equals("inputA")) { // 328741
				assertTrue(interceptors.length == 10);
				assertEquals("interceptor-three", interceptors[0].toString());
				assertEquals("interceptor-two", interceptors[1].toString());
				assertEquals("interceptor-eight", interceptors[2].toString());
				assertEquals("interceptor-seven", interceptors[3].toString());
				assertEquals("interceptor-five", interceptors[4].toString());
				assertEquals("interceptor-six", interceptors[5].toString());
				assertEquals("interceptor-ten", interceptors[6].toString());
				assertEquals("interceptor-eleven", interceptors[7].toString());
				assertEquals("interceptor-four", interceptors[8].toString());
				assertEquals("interceptor-one", interceptors[9].toString());
			}
			else if (channelName.equals("inputB")) {
				assertTrue(interceptors.length == 6);
				assertEquals("interceptor-three", interceptors[0].toString());
				assertEquals("interceptor-two", interceptors[1].toString());
				assertEquals("interceptor-ten", interceptors[2].toString());
				assertEquals("interceptor-eleven", interceptors[3].toString());
				assertEquals("interceptor-four", interceptors[4].toString());
				assertEquals("interceptor-one", interceptors[5].toString());
			}
			else if (channelName.equals("foo")) {
				assertTrue(interceptors.length == 6);
				assertEquals("interceptor-two", interceptors[0].toString());
				assertEquals("interceptor-five", interceptors[1].toString());
				assertEquals("interceptor-ten", interceptors[2].toString());
				assertEquals("interceptor-eleven", interceptors[3].toString());
				assertEquals("interceptor-four", interceptors[4].toString());
				assertEquals("interceptor-one", interceptors[5].toString());
			}
			else if (channelName.equals("bar")) {
				assertTrue(interceptors.length == 4);
				assertEquals("interceptor-eight", interceptors[0].toString());
				assertEquals("interceptor-seven", interceptors[1].toString());
				assertEquals("interceptor-ten", interceptors[2].toString());
				assertEquals("interceptor-eleven", interceptors[3].toString());
			}
			else if (channelName.equals("baz")) {
				assertTrue(interceptors.length == 2);
				assertEquals("interceptor-ten", interceptors[0].toString());
				assertEquals("interceptor-eleven", interceptors[1].toString());
			}
			else if (channelName.equals("inputWithProxy")) {
				assertTrue(interceptors.length == 6);
			}
			else if (channelName.equals("test")) {
				assertNotNull(interceptors);
				assertTrue(interceptors.length == 2);
				List<String> interceptorNames = new ArrayList<String>();
				for (ChannelInterceptor interceptor : interceptors) {
					interceptorNames.add(interceptor.toString());
				}
				assertTrue(interceptorNames.contains("interceptor-ten"));
				assertTrue(interceptorNames.contains("interceptor-eleven"));
			}
		}
	}

	@Test
	public void testWildCardPatternMatch() {

		List<ChannelInterceptor> channelInterceptors = this.inputCChannel.getChannelInterceptors();
		List<String> interceptorNames = new ArrayList<String>();
		for (ChannelInterceptor interceptor : channelInterceptors) {
			interceptorNames.add(interceptor.toString());
		}
		assertTrue(interceptorNames.contains("interceptor-ten"));
		assertTrue(interceptorNames.contains("interceptor-eleven"));
	}

	@Test
	public void testDynamicMessageChannelBeanWithAutoGlobalChannelInterceptor() {
		DirectChannel testChannel = new DirectChannel();
		ConfigurableListableBeanFactory beanFactory = this.applicationContext.getBeanFactory();
		beanFactory.initializeBean(testChannel, "testChannel");

		List<ChannelInterceptor> channelInterceptors = testChannel.getChannelInterceptors();

		assertEquals(2, channelInterceptors.size());
		assertThat(channelInterceptors.get(0), instanceOf(SampleInterceptor.class));
		assertThat(channelInterceptors.get(0), instanceOf(SampleInterceptor.class));
	}

	public static class SampleInterceptor implements ChannelInterceptor {

		private String testIdentifier;

		public String getTestIdentifier() {
			return testIdentifier;
		}

		public void setTestIdentifier(String testIdentifier) {
			this.testIdentifier = testIdentifier;
		}

		@Override
		public Message<?> postReceive(Message<?> message, MessageChannel channel) {
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
		public Message<?> preSend(Message<?> message, MessageChannel channel) {
			return null;
		}

		@Override
		public String toString() {
			return "interceptor-" + testIdentifier;
		}

	}


	public static class SampleOrderedInterceptor extends SampleInterceptor implements Ordered {

		private int order;

		@Override
		public int getOrder() {
			return order;
		}

		public void setOrder(int order) {
			this.order = order;
		}

	}

	public static class TestInterceptor implements MethodInterceptor {

		@Override
		public Object invoke(MethodInvocation invocation) throws Throwable {
			return invocation.proceed();
		}

	}

}
