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
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import org.springframework.integration.message.Message;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 */
public class RoutingBarrierTests {

	@Test
	public void testBasicCompletionCheck() {
		RoutingBarrier barrier = new RoutingBarrier(new TwoMessageCompletionStrategy());
		barrier.addMessage(new StringMessage("test1"));
		assertFalse(barrier.isComplete());
		barrier.addMessage(new StringMessage("test2"));
		assertTrue(barrier.isComplete());
	}

	@Test
	public void testMessageRetrieval() {
		RoutingBarrier barrier = new RoutingBarrier(new TwoMessageCompletionStrategy());
		barrier.addMessage(new StringMessage("test1"));
		assertEquals(1, barrier.getMessages().size());
		barrier.addMessage(new StringMessage("test2"));
		assertEquals(2, barrier.getMessages().size());
	}

	@Test
	public void testWaitForCompletionTimesOut() {
		RoutingBarrier barrier = new RoutingBarrier(new TwoMessageCompletionStrategy());
		barrier.addMessage(new StringMessage("test1"));
		assertFalse(barrier.isComplete());
		assertFalse(barrier.waitForCompletion(10));
	}

	@Test
	public void testWaitForCompletionReturnsTrueImmediately() {
		RoutingBarrier barrier = new RoutingBarrier(new TwoMessageCompletionStrategy());
		barrier.addMessage(new StringMessage("test1"));
		assertFalse(barrier.isComplete());
		barrier.addMessage(new StringMessage("test2"));
		assertTrue(barrier.waitForCompletion(0));
		assertTrue(barrier.isComplete());
	}

	@Test
	public void testEmptyMessageList() {
		RoutingBarrier barrier = new RoutingBarrier(new TwoMessageCompletionStrategy());
		assertFalse(barrier.isComplete());
		assertFalse(barrier.waitForCompletion(0));
		assertEquals(0, barrier.getMessages().size());
	}


	private static class TwoMessageCompletionStrategy implements RoutingBarrierCompletionStrategy {

		public boolean isComplete(List<Message<?>> messages) {
			return (messages.size() == 2);
		}
	}

}
