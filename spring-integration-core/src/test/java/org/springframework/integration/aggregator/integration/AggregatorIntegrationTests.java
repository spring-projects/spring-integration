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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.MessageHeaders;
import org.springframework.integration.core.PollableChannel;
import org.springframework.integration.message.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

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
	@Qualifier("expiringAggregatorInput")
	private MessageChannel expiringAggregatorInput;
	
	@Autowired
	@Qualifier("nonExpiringAggregatorInput")
	private MessageChannel nonExpiringAggregatorInput;

	@Autowired
	@Qualifier("output")
	private PollableChannel output;
	
	@Autowired
	@Qualifier("discard")
	private PollableChannel discard;

	@Test//(timeout=5000)
	public void testVanillaAggregation() throws Exception {
		for (int i = 0; i < 5; i++) {
			Map<String, Object> headers = stubHeaders(i, 5, 1);
			input.send(new GenericMessage<Integer>(i, headers));
		}
		assertEquals(0 + 1 + 2 + 3 + 4, output.receive().getPayload());
	}
	
	@Test
	public void testNonExpiringAggregator() throws Exception {
		for (int i = 0; i < 5; i++) {
			Map<String, Object> headers = stubHeaders(i, 5, 1);
			nonExpiringAggregatorInput.send(new GenericMessage<Integer>(i, headers));
		}
		assertNotNull(output.receive(0));
		
		assertNull(discard.receive(0));
		
		for (int i = 5; i < 10; i++) {
			Map<String, Object> headers = stubHeaders(i, 5, 1);
			nonExpiringAggregatorInput.send(new GenericMessage<Integer>(i, headers));
		}
		assertNull(output.receive(0));
		
		assertNotNull(discard.receive(0));
		assertNotNull(discard.receive(0));
		assertNotNull(discard.receive(0));
		assertNotNull(discard.receive(0));
		assertNotNull(discard.receive(0));
	}
	
	@Test
	public void testExpiringAggregator() throws Exception {
		for (int i = 0; i < 5; i++) {
			Map<String, Object> headers = stubHeaders(i, 5, 1);
			expiringAggregatorInput.send(new GenericMessage<Integer>(i, headers));
		}
		assertNotNull(output.receive(0));
		
		assertNull(discard.receive(0));
		
		for (int i = 5; i < 10; i++) {
			Map<String, Object> headers = stubHeaders(i, 5, 1);
			expiringAggregatorInput.send(new GenericMessage<Integer>(i, headers));
		}
		assertNotNull(output.receive(0));
		
		assertNull(discard.receive(0));

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
