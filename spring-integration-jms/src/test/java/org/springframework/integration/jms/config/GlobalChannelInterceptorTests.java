/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.integration.jms.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.Test;

import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.InterceptableChannel;

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
		try (ConfigurableApplicationContext context = new ClassPathXmlApplicationContext(
				"GlobalChannelInterceptorTests-context.xml", GlobalChannelInterceptorTests.class)) {

			InterceptableChannel jmsChannel = context.getBean("jmsChannel", AbstractMessageChannel.class);
			List<ChannelInterceptor> interceptors = jmsChannel.getInterceptors();
			assertThat(interceptors).isNotNull();
			assertThat(interceptors.size()).isEqualTo(1);
			assertThat(interceptors.get(0) instanceof SampleInterceptor).isTrue();
		}
	}


	public static class SampleInterceptor implements ChannelInterceptor {

	}

}
