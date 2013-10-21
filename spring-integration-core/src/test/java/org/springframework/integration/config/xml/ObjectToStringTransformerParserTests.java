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

package org.springframework.integration.config.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Mark Fisher
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class ObjectToStringTransformerParserTests {

	@Autowired
	@Qualifier("directInput")
	private MessageChannel directInput;

	@Autowired
	@Qualifier("queueInput")
	private MessageChannel queueInput;

	@Autowired
	@Qualifier("output")
	private PollableChannel output;

	@Autowired
	private AbstractEndpoint withCharset;

	@Test
	public void directChannelWithStringMessage() {
		directInput.send(new GenericMessage<String>("foo"));
		Message<?> result = output.receive(0);
		assertNotNull(result);
		assertEquals("foo", result.getPayload());
	}

	@Test
	public void queueChannelWithStringMessage() {
		queueInput.send(new GenericMessage<String>("foo"));
		Message<?> result = output.receive(3000);
		assertNotNull(result);
		assertEquals("foo", result.getPayload());
	}

	@Test
	public void directChannelWithObjectMessage() {
		directInput.send(new GenericMessage<TestBean>(new TestBean()));
		Message<?> result = output.receive(0);
		assertNotNull(result);
		assertEquals("test", result.getPayload());
	}

	@Test
	public void queueChannelWithObjectMessage() {
		queueInput.send(new GenericMessage<TestBean>(new TestBean()));
		Message<?> result = output.receive(3000);
		assertNotNull(result);
		assertEquals("test", result.getPayload());
	}

	@Test
	public void charset() {
		assertEquals("FOO", TestUtils.getPropertyValue(this.withCharset, "handler.transformer.charset"));
	}

	private static class TestBean {

		@Override
		public String toString() {
			return "test";
		}
	}

}
