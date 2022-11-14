/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.integration.channel;

import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.dispatcher.RoundRobinLoadBalancingStrategy;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 */
public class DirectChannelParserTests {

	@Test
	public void testReceivesMessageFromChannelWithSource() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"directChannelParserTests.xml", DirectChannelParserTests.class);
		Object channel = context.getBean("channel");
		assertThat(channel.getClass()).isEqualTo(DirectChannel.class);
		DirectFieldAccessor dcAccessor = new DirectFieldAccessor(((DirectChannel) channel).getDispatcher());
		assertThat(dcAccessor.getPropertyValue("loadBalancingStrategy") instanceof RoundRobinLoadBalancingStrategy)
				.isTrue();
		context.close();
	}

}
