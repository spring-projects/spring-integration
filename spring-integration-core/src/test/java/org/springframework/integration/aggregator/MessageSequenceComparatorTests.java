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

import org.junit.Test;

import org.springframework.messaging.Message;
import org.springframework.integration.support.MessageBuilder;

/**
 * @author Mark Fisher
 */
public class MessageSequenceComparatorTests {

	@Test
	public void testLessThan() {
		MessageSequenceComparator comparator = new MessageSequenceComparator();
		Message<String> message1 = MessageBuilder.withPayload("test1")
				.setSequenceNumber(1).build();
		Message<String> message2 = MessageBuilder.withPayload("test2")
				.setSequenceNumber(2).build();
		assertEquals(-1, comparator.compare(message1, message2));
	}

	@Test
	public void testEqual() {
		MessageSequenceComparator comparator = new MessageSequenceComparator();
		Message<String> message1 = MessageBuilder.withPayload("test1")
				.setSequenceNumber(3).build();
		Message<String> message2 = MessageBuilder.withPayload("test2")
				.setSequenceNumber(3).build();
		assertEquals(0, comparator.compare(message1, message2));
	}

	@Test
	public void testGreaterThan() {
		MessageSequenceComparator comparator = new MessageSequenceComparator();
		Message<String> message1 = MessageBuilder.withPayload("test1")
				.setSequenceNumber(5).build();
		Message<String> message2 = MessageBuilder.withPayload("test2")
				.setSequenceNumber(3).build();
		assertEquals(1, comparator.compare(message1, message2));
	}

	@Test
	public void testEqualWithDefaultValues() {
		MessageSequenceComparator comparator = new MessageSequenceComparator();
		Message<String> message1 = MessageBuilder.withPayload("test1").build();
		Message<String> message2 = MessageBuilder.withPayload("test2").build();
		assertEquals(0, comparator.compare(message1, message2));
	}

}
