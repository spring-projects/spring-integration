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
package org.springframework.integration.aggregator;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.springframework.messaging.Message;
import org.springframework.integration.annotation.Header;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.util.ReflectionUtils;

/**
 * @author Dave Syer
 * 
 */
public class CorrelationStrategyAdapterTests {

	private Message<?> message;

	@Before
	public void init() {
		message = MessageBuilder.withPayload("foo").setHeader("a", "b").setHeader("c", "d").build();
	}

	@Test
	public void testMethodName() {
		MethodInvokingCorrelationStrategy adapter = new MethodInvokingCorrelationStrategy(new SimpleMessageCorrelator(), "getKey");
		assertEquals("b", adapter.getCorrelationKey(message));
	}

	@Test
	public void testCorrelationStrategyAdapterObjectMethod() {
		MethodInvokingCorrelationStrategy adapter = new MethodInvokingCorrelationStrategy(new SimpleMessageCorrelator(),
				ReflectionUtils.findMethod(SimpleMessageCorrelator.class, "getKey", Message.class));
		assertEquals("b", adapter.getCorrelationKey(message));
	}

	@Test
	public void testCorrelationStrategyAdapterPojoMethod() {
		MethodInvokingCorrelationStrategy adapter = new MethodInvokingCorrelationStrategy(new SimplePojoCorrelator(), "getKey");
		assertEquals("foo", adapter.getCorrelationKey(message));
	}

	@Test
	public void testHeaderPojoMethod() {
		MethodInvokingCorrelationStrategy adapter = new MethodInvokingCorrelationStrategy(new SimpleHeaderCorrelator(), "getKey");
		assertEquals("b", adapter.getCorrelationKey(message));
	}

	@Test
	public void testHeadersPojoMethod() {
		MethodInvokingCorrelationStrategy adapter = new MethodInvokingCorrelationStrategy(new MultiHeaderCorrelator(),
				ReflectionUtils.findMethod(MultiHeaderCorrelator.class, "getKey", String.class, String.class));
		assertEquals("bd", adapter.getCorrelationKey(message));
	}

	private static class MultiHeaderCorrelator {
		@SuppressWarnings("unused")
		public String getKey(@Header("a") String header, @Header("c") String other) {
			return header + other;
		}
	}

	private static class SimpleHeaderCorrelator {
		@SuppressWarnings("unused")
		public String getKey(@Header("a") String header) {
			return header;
		}
	}

	private static class SimplePojoCorrelator {
		@SuppressWarnings("unused")
		public String getKey(String message) {
			return message;
		}
	}

	private static class SimpleMessageCorrelator {
		@SuppressWarnings("unused")
		public String getKey(Message<?> message) {
			return (String) message.getHeaders().get("a");
		}
	}

}
