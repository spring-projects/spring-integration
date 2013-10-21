/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.Map;

import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.integration.channel.interceptor.WireTap;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Mark Fisher
 */
public class WireTapParserTests {

	@Test
	public void simpleWireTap() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"wireTapParserTests.xml", this.getClass());
		MessageChannel mainChannel = (MessageChannel) context.getBean("noSelectors");
		PollableChannel wireTapChannel = (PollableChannel) context.getBean("wireTapChannel");
		assertNull(wireTapChannel.receive(0));
		Message<?> original = new GenericMessage<String>("test");
		mainChannel.send(original);
		Message<?> intercepted = wireTapChannel.receive(0);
		assertNotNull(intercepted);
		assertEquals(original, intercepted);
	}

	@Test
	public void simpleWireTapWithId() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"wireTapParserTests.xml", this.getClass());
		WireTap wireTap = (WireTap) context.getBean("wireTap");
		assertNotNull(wireTap);
	}

	@Test
	public void wireTapWithAcceptingSelector() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"wireTapParserTests.xml", this.getClass());
		MessageChannel mainChannel = (MessageChannel) context.getBean("accepting");
		PollableChannel wireTapChannel = (PollableChannel) context.getBean("wireTapChannel");
		assertNull(wireTapChannel.receive(0));
		Message<?> original = new GenericMessage<String>("test");
		mainChannel.send(original);
		Message<?> intercepted = wireTapChannel.receive(0);
		assertNotNull(intercepted);
		assertEquals(original, intercepted);
	}

	@Test
	public void wireTapWithRejectingSelector() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"wireTapParserTests.xml", this.getClass());
		MessageChannel mainChannel = (MessageChannel) context.getBean("rejecting");
		PollableChannel wireTapChannel = (PollableChannel) context.getBean("wireTapChannel");
		assertNull(wireTapChannel.receive(0));
		Message<?> original = new GenericMessage<String>("test");
		mainChannel.send(original);
		Message<?> intercepted = wireTapChannel.receive(0);
		assertNull(intercepted);
	}

	@Test
	public void wireTapTimeouts() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"wireTapParserTests.xml", this.getClass());
		Map<String, WireTap> beans = context.getBeansOfType(WireTap.class);
		int defaultTimeoutCount = 0;
		int expectedTimeoutCount = 0;
		int otherTimeoutCount = 0;
		for (WireTap wireTap : beans.values()) {
			long timeout = ((Long) new DirectFieldAccessor(wireTap).getPropertyValue("timeout")).longValue();
			if (timeout == 0) {
				defaultTimeoutCount++;
			}
			else if (timeout == 1234) {
				expectedTimeoutCount++;
			}
			else {
				otherTimeoutCount++;
			}
		}
		assertEquals(4, defaultTimeoutCount);
		assertEquals(1, expectedTimeoutCount);
		assertEquals(0, otherTimeoutCount);
	}

}
