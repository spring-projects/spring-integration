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

import org.junit.Test;

import org.springframework.integration.message.StringMessage;

/**
 * @author Mark Fisher
 */
public class MessageSequenceComparatorTests {

	@Test
	public void testLessThan() {
		MessageSequenceComparator comparator = new MessageSequenceComparator();
		StringMessage message1 = new StringMessage("test1");
		message1.getHeader().setSequenceNumber(1);
		StringMessage message2 = new StringMessage("test2");
		message2.getHeader().setSequenceNumber(2);
		assertEquals(-1, comparator.compare(message1, message2));
	}

	@Test
	public void testEqual() {
		MessageSequenceComparator comparator = new MessageSequenceComparator();
		StringMessage message1 = new StringMessage("test1");
		message1.getHeader().setSequenceNumber(3);
		StringMessage message2 = new StringMessage("test2");
		message2.getHeader().setSequenceNumber(3);
		assertEquals(0, comparator.compare(message1, message2));
	}

	@Test
	public void testGreaterThan() {
		MessageSequenceComparator comparator = new MessageSequenceComparator();
		StringMessage message1 = new StringMessage("test1");
		message1.getHeader().setSequenceNumber(5);
		StringMessage message2 = new StringMessage("test2");
		message2.getHeader().setSequenceNumber(3);
		assertEquals(1, comparator.compare(message1, message2));
	}

	@Test
	public void testEqualWithDefaultValues() {
		MessageSequenceComparator comparator = new MessageSequenceComparator();
		StringMessage message1 = new StringMessage("test1");
		StringMessage message2 = new StringMessage("test2");
		assertEquals(0, comparator.compare(message1, message2));
	}

}
