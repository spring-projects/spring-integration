/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.channel.interceptor;

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
import org.springframework.integration.channel.DirectChannel;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.InterceptableChannel;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

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
	InterceptableChannel inputCChannel;

	@Test
	public void validateGlobalInterceptor() {
		Map<String, InterceptableChannel> channels = applicationContext.getBeansOfType(InterceptableChannel.class);
		for (String channelName : channels.keySet()) {
			InterceptableChannel channel = channels.get(channelName);
			if (channelName.equals("nullChannel")) {
				continue;
			}

			ChannelInterceptor[] interceptors = channel.getInterceptors()
					.toArray(new ChannelInterceptor[channel.getInterceptors().size()]);
			if (channelName.equals("inputA")) { // 328741
				assertThat(interceptors.length == 10).isTrue();
				assertThat(interceptors[0].toString()).isEqualTo("interceptor-three");
				assertThat(interceptors[1].toString()).isEqualTo("interceptor-two");
				assertThat(interceptors[2].toString()).isEqualTo("interceptor-eight");
				assertThat(interceptors[3].toString()).isEqualTo("interceptor-seven");
				assertThat(interceptors[4].toString()).isEqualTo("interceptor-five");
				assertThat(interceptors[5].toString()).isEqualTo("interceptor-six");
				assertThat(interceptors[6].toString()).isEqualTo("interceptor-ten");
				assertThat(interceptors[7].toString()).isEqualTo("interceptor-eleven");
				assertThat(interceptors[8].toString()).isEqualTo("interceptor-four");
				assertThat(interceptors[9].toString()).isEqualTo("interceptor-one");
			}
			else if (channelName.equals("inputB")) {
				assertThat(interceptors.length == 6).isTrue();
				assertThat(interceptors[0].toString()).isEqualTo("interceptor-three");
				assertThat(interceptors[1].toString()).isEqualTo("interceptor-two");
				assertThat(interceptors[2].toString()).isEqualTo("interceptor-ten");
				assertThat(interceptors[3].toString()).isEqualTo("interceptor-eleven");
				assertThat(interceptors[4].toString()).isEqualTo("interceptor-four");
				assertThat(interceptors[5].toString()).isEqualTo("interceptor-one");
			}
			else if (channelName.equals("foo")) {
				assertThat(interceptors.length == 6).isTrue();
				assertThat(interceptors[0].toString()).isEqualTo("interceptor-two");
				assertThat(interceptors[1].toString()).isEqualTo("interceptor-five");
				assertThat(interceptors[2].toString()).isEqualTo("interceptor-ten");
				assertThat(interceptors[3].toString()).isEqualTo("interceptor-eleven");
				assertThat(interceptors[4].toString()).isEqualTo("interceptor-four");
				assertThat(interceptors[5].toString()).isEqualTo("interceptor-one");
			}
			else if (channelName.equals("bar")) {
				assertThat(interceptors.length == 4).isTrue();
				assertThat(interceptors[0].toString()).isEqualTo("interceptor-eight");
				assertThat(interceptors[1].toString()).isEqualTo("interceptor-seven");
				assertThat(interceptors[2].toString()).isEqualTo("interceptor-ten");
				assertThat(interceptors[3].toString()).isEqualTo("interceptor-eleven");
			}
			else if (channelName.equals("baz")) {
				assertThat(interceptors.length == 2).isTrue();
				assertThat(interceptors[0].toString()).isEqualTo("interceptor-ten");
				assertThat(interceptors[1].toString()).isEqualTo("interceptor-eleven");
			}
			else if (channelName.equals("inputWithProxy")) {
				assertThat(interceptors.length == 6).isTrue();
			}
			else if (channelName.equals("test")) {
				assertThat(interceptors).isNotNull();
				assertThat(interceptors.length == 2).isTrue();
				List<String> interceptorNames = new ArrayList<String>();
				for (ChannelInterceptor interceptor : interceptors) {
					interceptorNames.add(interceptor.toString());
				}
				assertThat(interceptorNames.contains("interceptor-ten")).isTrue();
				assertThat(interceptorNames.contains("interceptor-eleven")).isTrue();
			}
		}
	}

	@Test
	public void testWildCardPatternMatch() {

		List<ChannelInterceptor> channelInterceptors = this.inputCChannel.getInterceptors();
		List<String> interceptorNames = new ArrayList<String>();
		for (ChannelInterceptor interceptor : channelInterceptors) {
			interceptorNames.add(interceptor.toString());
		}
		assertThat(interceptorNames.contains("interceptor-ten")).isTrue();
		assertThat(interceptorNames.contains("interceptor-eleven")).isTrue();
	}

	@Test
	public void testDynamicMessageChannelBeanWithAutoGlobalChannelInterceptor() {
		DirectChannel testChannel = new DirectChannel();
		ConfigurableListableBeanFactory beanFactory = this.applicationContext.getBeanFactory();
		beanFactory.initializeBean(testChannel, "testChannel");

		List<ChannelInterceptor> channelInterceptors = testChannel.getInterceptors();

		assertThat(channelInterceptors.size()).isEqualTo(2);
		assertThat(channelInterceptors.get(0)).isInstanceOf(SampleInterceptor.class);
		assertThat(channelInterceptors.get(0)).isInstanceOf(SampleInterceptor.class);
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
