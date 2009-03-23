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

package org.springframework.integration.aggregator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.LinkedHashSet;

import org.junit.Test;
import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 */
@SuppressWarnings("unchecked")
public class MessageBarrierTests {


	@Test
	public void testMessageRetrieval() {
		MessageBarrier barrier = new MessageBarrier(new LinkedHashSet(), null);
		barrier.getMessages().add(new StringMessage("test1"));
		assertEquals(1, barrier.getMessages().size());
		barrier.getMessages().add(new StringMessage("test2"));
		assertEquals(2, barrier.getMessages().size());
	}

	@Test
	public void testTimestamp() {
		long before = System.currentTimeMillis();
		MessageBarrier barrier = new MessageBarrier(new LinkedHashSet(), null);
		long timestamp = barrier.getTimestamp();
		assertTrue(before <= timestamp);
		long after = System.currentTimeMillis();
		assertTrue(after >= timestamp);
	}

	@Test
	public void testEmptyMessageList() {
		MessageBarrier barrier = new MessageBarrier(new LinkedHashSet(), null);
		assertEquals(0, barrier.getMessages().size());
	}

}
