/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.integration.router.config;

import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Oleg Zhurakousky
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class HeaderValueRouterParserTests {

	@Autowired
	private ConfigurableApplicationContext context;

	@Autowired
	private TestServiceA testServiceA;

	@Autowired
	private TestServiceB testServiceB;


	@Test
	public void testHeaderValuesAsChannels() {
		context.start();
		MessageBuilder<?> channel1MessageBuilder = MessageBuilder.withPayload("");
		channel1MessageBuilder.setHeader("testHeader", "channel1");
		Message<?> message1 = channel1MessageBuilder.build();
		MessageBuilder<?> channel2MessageBuilder = MessageBuilder.withPayload("");
		channel2MessageBuilder.setHeader("testHeader", "channel2");
		Message<?> message2 = channel2MessageBuilder.build();
		testServiceA.foo(message1);
		testServiceA.foo(message2);
		PollableChannel channel1 = (PollableChannel) context.getBean("channel1");
		PollableChannel channel2 = (PollableChannel) context.getBean("channel2");
		message1 = channel1.receive();
		assertTrue(message1.getHeaders().get("testHeader").equals("channel1"));
		message2 = channel2.receive();
		assertTrue(message2.getHeaders().get("testHeader").equals("channel2"));
	}
	@Test
	public void testHeaderValuesWithMapResolver() {
		context.start();
		MessageBuilder<?> channel1MessageBuilder = MessageBuilder.withPayload("");
		channel1MessageBuilder.setHeader("testHeader", "1");
		Message<?> message1 = channel1MessageBuilder.build();
		MessageBuilder<?> channel2MessageBuilder = MessageBuilder.withPayload("");
		channel2MessageBuilder.setHeader("testHeader", "2");
		Message<?> message2 = channel2MessageBuilder.build();
		testServiceB.foo(message1);
		testServiceB.foo(message2);
		PollableChannel channel1 = (PollableChannel) context.getBean("channel1");
		PollableChannel channel2 = (PollableChannel) context.getBean("channel2");
		message1 = channel1.receive();
		assertTrue(message1.getHeaders().get("testHeader").equals("1"));
		message2 = channel2.receive();
		assertTrue(message2.getHeaders().get("testHeader").equals("2"));
	}


	public static interface TestServiceA {
		public void foo(Message<?> message);
	}

	public static interface TestServiceB {
		public void foo(Message<?> message);
	}

}
