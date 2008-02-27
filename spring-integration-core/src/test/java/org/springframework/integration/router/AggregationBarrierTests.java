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

package org.springframework.integration.router;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import org.springframework.integration.message.Message;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 */
public class AggregationBarrierTests {

	@Test
	public void testBasicCompletionCheck() {
		AggregationBarrier barrier = new AggregationBarrier(new TwoMessageCompletionStrategy());
		assertNull(barrier.addAndRelease(new StringMessage("test1")));
		assertNotNull(barrier.addAndRelease(new StringMessage("test2")));
	}

	@Test
	public void testMessageRetrieval() {
		AggregationBarrier barrier = new AggregationBarrier(new TwoMessageCompletionStrategy());
		barrier.addAndRelease(new StringMessage("test1"));
		assertEquals(1, barrier.getMessages().size());
		barrier.addAndRelease(new StringMessage("test2"));
		assertEquals(2, barrier.getMessages().size());
	}

	@Test
	public void testTimestamp() {
		long before = System.currentTimeMillis();
		AggregationBarrier barrier = new AggregationBarrier(new TwoMessageCompletionStrategy());
		long timestamp = barrier.getTimestamp();
		assertTrue(before <= timestamp);
		long after = System.currentTimeMillis();
		assertTrue(after >= timestamp);
	}

	@Test
	public void testEmptyMessageList() {
		AggregationBarrier barrier = new AggregationBarrier(new TwoMessageCompletionStrategy());
		assertEquals(0, barrier.getMessages().size());
	}


	private static class TwoMessageCompletionStrategy implements CompletionStrategy {

		public boolean isComplete(List<Message<?>> messages) {
			return (messages.size() == 2);
		}
	}

}
