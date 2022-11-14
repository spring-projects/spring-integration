/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.integration.aggregator.scenarios;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Iwein Fuld
 * @author Gary Russell
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
public class PartialSequencesWithGapsTests {

	@Autowired
	MessageChannel in;

	@Autowired
	SubscribableChannel out;

	@SuppressWarnings("rawtypes")
	Queue<Message> received = new ArrayBlockingQueue<Message>(10);

	@Before
	public void collectOutput() {
		out.subscribe(message -> received.add(message));
	}

	@Test
	public void shouldNotReleaseAfterGap() {
		in.send(message(6, 6));
		in.send(message(2, 6));
		in.send(message(1, 6));
		assertThat(new IntegrationMessageHeaderAccessor(received.poll()).getSequenceNumber()).isEqualTo(1);
		assertThat(new IntegrationMessageHeaderAccessor(received.poll()).getSequenceNumber()).isEqualTo(2);
		received.poll();
		received.poll();
		in.send(message(5, 6));
		assertThat(received.poll()).isNull();
		in.send(message(4, 6));
		assertThat(received.poll()).isNull();
	}

	private Message<?> message(int sequenceNumber, int sequenceSize) {
		return MessageBuilder.withPayload("foo")
				.setSequenceNumber(sequenceNumber)
				.setSequenceSize(sequenceSize)
				.setCorrelationId("foo").build();
	}

}
