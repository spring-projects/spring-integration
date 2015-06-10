/*
 * Copyright 2015 the original author or authors.
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
import org.springframework.integration.endpoint.PollingConsumer;
import org.springframework.integration.handler.SuspendingMessageHandler;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gary Russell
 * @since 4.2
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class BarrierParserTests {

	@Autowired
	private MessageChannel in;

	@Autowired
	private MessageChannel release;

	@Autowired
	private PollableChannel out;

	@Autowired
	private PollingConsumer endpoint;

	@Test
	public void parserTests() {
		this.in.send(new GenericMessage<String>("foo"));
		this.release.send(new GenericMessage<String>("bar"));
		Message<?> received = out.receive(10000);
		assertNotNull(received);
		this.endpoint.stop();

		SuspendingMessageHandler handler = TestUtils.getPropertyValue(this.endpoint, "handler",
				SuspendingMessageHandler.class);
		assertEquals(10000L, TestUtils.getPropertyValue(handler, "timeout"));
	}

}
