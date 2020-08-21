/*
 * Copyright 2015-2020 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.config.xml;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

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
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.2
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class BarrierParserTests {

	@Autowired
	private MessageChannel in;

	@Autowired
	private MessageChannel release;

	@Autowired
	private PollableChannel out;

	@Autowired
	private PollableChannel discards;

	@Autowired
	private PollingConsumer barrier1;

	@Autowired
	private PollingConsumer barrier2;

	@Autowired
	private EventDrivenConsumer barrier3;

	@Test
	public void parserTestsWithMessage() {
		this.in.send(new GenericMessage<>("foo"));
		this.release.send(new GenericMessage<>("bar"));
		Message<?> received = out.receive(10000);
		assertThat(received).isNotNull();
		this.barrier1.stop();
	}

	@Test
	public void parserFieldPopulationTests() {
		BarrierMessageHandler handler = TestUtils.getPropertyValue(this.barrier1, "handler",
				BarrierMessageHandler.class);
		assertThat(TestUtils.getPropertyValue(handler, "requestTimeout")).isEqualTo(10000L);
		assertThat(TestUtils.getPropertyValue(handler, "triggerTimeout")).isEqualTo(5000L);
		assertThat(TestUtils.getPropertyValue(handler, "requiresReply", Boolean.class)).isTrue();
		assertThat(TestUtils.getPropertyValue(this.barrier2, "handler.correlationStrategy"))
				.isInstanceOf(HeaderAttributeCorrelationStrategy.class);
		assertThat(TestUtils.getPropertyValue(this.barrier3, "handler.messageGroupProcessor"))
				.isInstanceOf(TestMGP.class);
		assertThat(TestUtils.getPropertyValue(this.barrier3, "handler.correlationStrategy")).isInstanceOf(TestCS.class);
		assertThat(this.discards).isSameAs(handler.getDiscardChannel());
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
