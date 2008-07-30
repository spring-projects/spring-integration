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

package org.springframework.integration.router.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.PollableChannel;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 */
public class RouterParserTests {

	@Test
	public void testRouter() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"routerParserTests.xml", this.getClass());
		context.start();
		MessageChannel input = (MessageChannel) context.getBean("input");
		PollableChannel output1 = (PollableChannel) context.getBean("output1");
		PollableChannel output2 = (PollableChannel) context.getBean("output2");
		input.send(new StringMessage("1"));
		Message<?> result1 = output1.receive(1000);
		assertEquals("1", result1.getPayload());
		assertNull(output2.receive(0));
		input.send(new StringMessage("2"));
		Message<?> result2 = output2.receive(1000);
		assertEquals("2", result2.getPayload());
		assertNull(output1.receive(0));
	}

}
