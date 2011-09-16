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

package org.springframework.integration.aggregator.integration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.MessageHeaders;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

/**
 * @author Iwein Fuld
 * @author Alex Peters
 * @author Oleg Zhurakousky
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class AggregatorIntegrationTests {

	@Autowired
	@Qualifier("input")
	private MessageChannel input;

	@Autowired
	@Qualifier("output")
	private PollableChannel output;

	@Test//(timeout=5000)
	public void testVanillaAggregation() throws Exception {
		for (int i = 0; i < 5; i++) {
			Map<String, Object> headers = stubHeaders(i, 5, 1);
			input.send(new GenericMessage<Integer>(i, headers));
		}
		assertEquals(0 + 1 + 2 + 3 + 4, output.receive().getPayload());
	}
	
	@Test
	public void testAggregatorWithCustomReleaseStrategyAndPartialCompletion() throws Exception {
		ApplicationContext context = new ClassPathXmlApplicationContext("AggregatorWithPartialReleaseOnCompletion.xml", this.getClass());
		MessageChannel inputChannel = context.getBean("inputChannel", MessageChannel.class);
		QueueChannel replyChannel = new QueueChannel();
		QueueChannel discardChannel = context.getBean("discardChannel", QueueChannel.class);
		for (int i = 0; i < 10; i++) {
			Message<?> message = MessageBuilder.withPayload(i).setReplyChannel(replyChannel).setCorrelationId(1).setSequenceNumber(i).setSequenceSize(10).build();
			inputChannel.send(message);
		}
		assertEquals(4, ((List<?>)((Message<?>)replyChannel.receive(100)).getPayload()).size());
		assertEquals(4, ((List<?>)((Message<?>)replyChannel.receive(100)).getPayload()).size());
		assertEquals(2, ((List<?>)((Message<?>)replyChannel.receive(100)).getPayload()).size());
		
		assertNull(discardChannel.receive(100));
	}

	// configured in context associated with this test
	public static class SummingAggregator {
		public Integer sum(List<Integer> numbers) {
			int result = 0;
			for (Integer number : numbers) {
				result += number;
			}
			return result;
		}
	}

	private Map<String, Object> stubHeaders(int sequenceNumber, int sequenceSize, int correllationId) {
		Map<String, Object> headers = new HashMap<String, Object>();
		headers.put(MessageHeaders.SEQUENCE_NUMBER, sequenceNumber);
		headers.put(MessageHeaders.SEQUENCE_SIZE, sequenceSize);
		headers.put(MessageHeaders.CORRELATION_ID, correllationId);
		return headers;
	}

}
