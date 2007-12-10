/*
 * Copyright 2002-2007 the original author or authors.
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

package org.springframework.integration.endpoint;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.endpoint.OutboundMethodInvokingChannelAdapter;
import org.springframework.integration.message.DocumentMessage;

/**
 * @author Mark Fisher
 */
public class OutboundChannelAdapterTests {

	@Test
	public void testValidMethod() {
		OutboundMethodInvokingChannelAdapter<TestSink> adapter =
				new OutboundMethodInvokingChannelAdapter<TestSink>();
		adapter.setObject(new TestSink());
		adapter.setMethod("validMethod");
		adapter.afterPropertiesSet();
		boolean result = adapter.send(new DocumentMessage(1, "test"));
		assertTrue(result);
	}

	@Test(expected=MessageHandlingException.class)
	public void testInvalidMethodWithNoArgs() {
		OutboundMethodInvokingChannelAdapter<TestSink> adapter =
				new OutboundMethodInvokingChannelAdapter<TestSink>();
		adapter.setObject(new TestSink());
		adapter.setMethod("invalidMethodWithNoArgs");
		adapter.afterPropertiesSet();
		adapter.send(new DocumentMessage(1, "test"));
	}

	@Test
	public void testValidMethodWithIgnoredReturnValue() {
		OutboundMethodInvokingChannelAdapter<TestSink> adapter =
				new OutboundMethodInvokingChannelAdapter<TestSink>();
		adapter.setObject(new TestSink());
		adapter.setMethod("validMethodWithIgnoredReturnValue");
		adapter.afterPropertiesSet();
		boolean result = adapter.send(new DocumentMessage(1, "test"));
		assertTrue(result);
	}

	@Test(expected=MessageHandlingException.class)
	public void testNoMatchingMethodName() {
		OutboundMethodInvokingChannelAdapter<TestSink> adapter =
				new OutboundMethodInvokingChannelAdapter<TestSink>();
		adapter.setObject(new TestSink());
		adapter.setMethod("noSuchMethod");
		adapter.send(new DocumentMessage(1, "test"));
	}

}
