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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.endpoint.InboundMethodInvokingChannelAdapter;
import org.springframework.integration.message.Message;

/**
 * @author Mark Fisher
 */
public class InboundChannelAdapterTests {

	@Test
	public void testValidMethod() {
		InboundMethodInvokingChannelAdapter<TestSource> adapter =
				new InboundMethodInvokingChannelAdapter<TestSource>();
		adapter.setObject(new TestSource());
		adapter.setMethod("validMethod");
		adapter.afterPropertiesSet();
		Message message = adapter.receive();
		assertEquals("valid", message.getPayload());
	}

	@Test(expected=MessageHandlingException.class)
	public void testInvalidMethodWithArg() {
		InboundMethodInvokingChannelAdapter<TestSource> adapter =
				new InboundMethodInvokingChannelAdapter<TestSource>();
		adapter.setObject(new TestSource());
		adapter.setMethod("invalidMethodWithArg");
		adapter.afterPropertiesSet();
		adapter.receive();
	}

	@Test(expected=MessageHandlingException.class)
	public void testInvalidMethodWithNoReturnValue() {
		InboundMethodInvokingChannelAdapter<TestSource> adapter =
				new InboundMethodInvokingChannelAdapter<TestSource>();
		adapter.setObject(new TestSource());
		adapter.setMethod("invalidMethodWithNoReturnValue");
		adapter.afterPropertiesSet();
		adapter.receive();
	}

	@Test(expected=MessageHandlingException.class)
	public void testNoMatchingMethodName() {
		InboundMethodInvokingChannelAdapter<TestSource> adapter =
				new InboundMethodInvokingChannelAdapter<TestSource>();
		adapter.setObject(new TestSource());
		adapter.setMethod("noSuchMethod");
		adapter.afterPropertiesSet();
		adapter.receive();		
	}

}
