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

package org.springframework.integration.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Set;

import org.junit.Test;

/**
 * @author Mark Fisher
 */
public class MessageHeaderTests {

	@Test
	public void testTimestamp() {
		MessageHeader header = new MessageHeader();
		assertNotNull(header.getTimestamp());
	}

	@Test
	public void testAttributes() {
		MessageHeader header = new MessageHeader();
		Integer integer = new Integer(123);
		header.setAttribute("test", integer);
		assertEquals(integer, header.getAttribute("test"));
		assertNull(header.getAttribute("nosuchattribute"));
		Set<String> names = header.getAttributeNames();
		assertEquals(1, names.size());
		assertTrue(names.contains("test"));
	}

	@Test
	public void testProperties() {
		MessageHeader header = new MessageHeader();
		header.setProperty("foo", "bar");
		assertEquals("bar", header.getProperty("foo"));
		assertNull(header.getProperty("nosuchproperty"));
		Set<String> names = header.getPropertyNames();
		assertEquals(1, names.size());
		assertTrue(names.contains("foo"));
	}

}
