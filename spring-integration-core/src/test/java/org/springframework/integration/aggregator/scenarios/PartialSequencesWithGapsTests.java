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
package org.springframework.integration.aggregator.scenarios;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.SubscribableChannel;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.Queue;
import java.util.concurrent.ArrayBlockingQueue;
import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.assertThat;

/**
 * @author Iwein Fuld
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
		out.subscribe(new MessageHandler() {
			public void handleMessage(Message<?> message) throws MessagingException {
				received.add(message);
			}
		});
	}

	@Test
	public void shouldNotReleaseAfterGap() {
		in.send(message(6, 6));
		in.send(message(2, 6));
		in.send(message(1, 6));
		assertThat(new IntegrationMessageHeaderAccessor(received.poll()).getSequenceNumber(), is(1));
		assertThat(new IntegrationMessageHeaderAccessor(received.poll()).getSequenceNumber(), is(2));
		received.poll();
		received.poll();
		in.send(message(5, 6));
		assertThat(received.poll(), is(nullValue()));
		in.send(message(4, 6));
		assertThat(received.poll(), is(nullValue()));
	}

	private Message<?> message(int sequenceNumber, int sequenceSize) {
		return MessageBuilder.withPayload("foo")
				.setSequenceNumber(sequenceNumber)
				.setSequenceSize(sequenceSize)
				.setCorrelationId("foo").build();
	}
}
