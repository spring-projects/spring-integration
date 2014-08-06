/*
 * Copyright 2002-2014 the original author or authors.
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
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.store.MessageGroupStore;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Iwein Fuld
 * @author Alex Peters
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @author Gary Russell
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class AggregatorIntegrationTests {

	@Autowired
	private ApplicationContext context;

	@Autowired
	private MessageChannel input;

	@Autowired
	private MessageChannel expiringAggregatorInput;

	@Autowired
	private MessageChannel nonExpiringAggregatorInput;

	@Autowired
	private MessageChannel groupTimeoutAggregatorInput;

	@Autowired
	private MessageChannel groupTimeoutExpressionAggregatorInput;

	@Autowired
	private PollableChannel output;

	@Autowired
	private PollableChannel discard;

	@Test//(timeout=5000)
	public void testVanillaAggregation() throws Exception {
		for (int i = 0; i < 5; i++) {
			Map<String, Object> headers = stubHeaders(i, 5, 1);
			input.send(new GenericMessage<Integer>(i, headers));
		}
		assertEquals(0 + 1 + 2 + 3 + 4, output.receive(1000).getPayload());
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

	@Test
	public void testGroupTimeoutScheduling() throws Exception {
		for (int i = 0; i < 5; i++) {
			Map<String, Object> headers = stubHeaders(i, 5, 1);
			this.groupTimeoutAggregatorInput.send(new GenericMessage<Integer>(i, headers));

			//Wait until 'group-timeout' does its stuff.
			MessageGroupStore mgs = TestUtils.getPropertyValue(this.context.getBean("gta.handler"), "messageStore",
					MessageGroupStore.class);
			int n = 0;
			while (n++ < 100 && mgs.getMessageGroupCount() > 0) {
				Thread.sleep(100);
			}
			assertTrue("Group did not complete", n < 100);
			assertNotNull(this.output.receive(1000));
			assertNull(this.discard.receive(0));
		}
	}


	@Test
	public void testGroupTimeoutExpressionScheduling() throws Exception {
		// Since group-timeout-expression="size() >= 2 ? 100 : -1". The first message won't be scheduled to 'forceComplete'
		this.groupTimeoutExpressionAggregatorInput.send(new GenericMessage<Integer>(1, stubHeaders(1, 6, 1)));
		assertNull(this.output.receive(0));
		assertNull(this.discard.receive(0));

		// As far as 'group.size() >= 2' it will be scheduled to 'forceComplete'
		this.groupTimeoutExpressionAggregatorInput.send(new GenericMessage<Integer>(2, stubHeaders(2, 6, 1)));
		assertNull(this.output.receive(0));
		Message<?> receive = this.output.receive(500);
		assertNotNull(receive);
		assertEquals(2, ((Collection<?>) receive.getPayload()).size());
		assertNull(this.discard.receive(0));

		// The same with these three messages
		this.groupTimeoutExpressionAggregatorInput.send(new GenericMessage<Integer>(3, stubHeaders(3, 6, 1)));
		assertNull(this.output.receive(0));
		assertNull(this.discard.receive(0));

		this.groupTimeoutExpressionAggregatorInput.send(new GenericMessage<Integer>(4, stubHeaders(4, 6, 1)));
		assertNull(this.output.receive(0));
		assertNull(this.discard.receive(0));

		this.groupTimeoutExpressionAggregatorInput.send(new GenericMessage<Integer>(5, stubHeaders(5, 6, 1)));
		assertNull(this.output.receive(0));
		receive = this.output.receive(500);
		assertNotNull(receive);
		assertEquals(3, ((Collection<?>) receive.getPayload()).size());
		assertNull(this.discard.receive(0));

		// The last message in the sequence - normal release by provided 'ReleaseStrategy'
		this.groupTimeoutExpressionAggregatorInput.send(new GenericMessage<Integer>(6, stubHeaders(6, 6, 1)));
		receive = this.output.receive(500);
		assertNotNull(receive);
		assertEquals(1, ((Collection<?>) receive.getPayload()).size());
		assertNull(this.discard.receive(0));
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

	private Map<String, Object> stubHeaders(int sequenceNumber, int sequenceSize, int correlationId) {
		Map<String, Object> headers = new HashMap<String, Object>();
		headers.put(IntegrationMessageHeaderAccessor.SEQUENCE_NUMBER, sequenceNumber);
		headers.put(IntegrationMessageHeaderAccessor.SEQUENCE_SIZE, sequenceSize);
		headers.put(IntegrationMessageHeaderAccessor.CORRELATION_ID, correlationId);
		return headers;
	}

}
