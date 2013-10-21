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
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;

/**
 * @author Mark Fisher
 */
public class SplitterAggregatorTests {

	private final AtomicInteger count = new AtomicInteger();


	@Test
	public void testSplitterAndAggregator() {
		ApplicationContext context = new ClassPathXmlApplicationContext(
				"splitterAggregatorTests.xml", this.getClass());
		MessageChannel inputChannel = (MessageChannel) context.getBean("numbers");
		PollableChannel outputChannel = (PollableChannel) context.getBean("results");
		inputChannel.send(new GenericMessage<Numbers>(this.nextTen()));
		Message<?> result1 = outputChannel.receive(1000);
		assertNotNull(result1);
		assertEquals(Integer.class, result1.getPayload().getClass());
		assertEquals(55, result1.getPayload());
		inputChannel.send(new GenericMessage<Numbers>(this.nextTen()));		
		Message<?> result2 = outputChannel.receive(1000);
		assertNotNull(result2);
		assertEquals(Integer.class, result2.getPayload().getClass());
		assertEquals(155, result2.getPayload());
	}

	private Numbers nextTen() {
		List<Integer> values = new ArrayList<Integer>();
		for (int i = 0; i < 10; i++) {
			values.add(this.count.incrementAndGet());
		}
		return new Numbers(values);
	}

}
