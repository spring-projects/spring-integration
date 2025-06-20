/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.channel.config;

import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.RendezvousChannel;
import org.springframework.messaging.MessageChannel;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Gary Russell
 */
public class RendezvousChannelParserTests {

	@Test
	public void testRendezvous() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"rendezvousChannelParserTests.xml", RendezvousChannelParserTests.class);
		MessageChannel channel = (MessageChannel) context.getBean("channel");
		assertThat(channel.getClass()).isEqualTo(RendezvousChannel.class);
		context.close();
	}

}
