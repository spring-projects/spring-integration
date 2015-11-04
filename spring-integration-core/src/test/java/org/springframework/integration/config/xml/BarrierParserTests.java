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

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.aggregator.BarrierMessageHandler;
import org.springframework.integration.aggregator.CorrelationStrategy;
import org.springframework.integration.aggregator.HeaderAttributeCorrelationStrategy;
import org.springframework.integration.aggregator.MessageGroupProcessor;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.endpoint.PollingConsumer;
import org.springframework.integration.store.MessageGroup;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gary Russell
 * @since 4.2
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class BarrierParserTests {

	@Autowired
	private MessageChannel in;

	@Autowired
	private MessageChannel release;

	@Autowired
	private PollableChannel out;

	@Autowired
	private PollingConsumer barrier1;

	@Autowired
	private PollingConsumer barrier2;

	@Autowired
	private EventDrivenConsumer barrier3;

	@Test
	public void parserTestsWithMessage() {
		this.in.send(new GenericMessage<String>("foo"));
		this.release.send(new GenericMessage<String>("bar"));
		Message<?> received = out.receive(10000);
		assertNotNull(received);
		this.barrier1.stop();
	}

	@Test
	public void parserFieldPopulationTests() {
		BarrierMessageHandler handler = TestUtils.getPropertyValue(this.barrier1, "handler",
				BarrierMessageHandler.class);
		assertEquals(10000L, TestUtils.getPropertyValue(handler, "timeout"));
		assertTrue(TestUtils.getPropertyValue(handler, "requiresReply", Boolean.class));
		assertThat(TestUtils.getPropertyValue(this.barrier2, "handler.correlationStrategy"),
				instanceOf(HeaderAttributeCorrelationStrategy.class));
		assertThat(TestUtils.getPropertyValue(this.barrier3, "handler.messageGroupProcessor"),
				instanceOf(TestMGP.class));
		assertThat(TestUtils.getPropertyValue(this.barrier3, "handler.correlationStrategy"),
				instanceOf(TestCS.class));
	}

	public static class TestMGP implements MessageGroupProcessor {

		@Override
		public Object processMessageGroup(MessageGroup group) {
			return null;
		}

	}

	public static class TestCS implements CorrelationStrategy {

		@Override
		public Object getCorrelationKey(Message<?> message) {
			return null;
		}

	}

}
