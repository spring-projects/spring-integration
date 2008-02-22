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
import static org.junit.Assert.assertFalse;
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
		Integer value = new Integer(123);
		Object previousValue = header.setAttribute("test", value);
		assertNull(previousValue);
		assertEquals(value, header.getAttribute("test"));
		assertNull(header.getAttribute("nosuchattribute"));
		Set<String> names = header.getAttributeNames();
		assertEquals(1, names.size());
		assertTrue(names.contains("test"));
		Integer newValue = new Integer(456);
		previousValue = header.setAttribute("test", newValue);
		assertEquals(value, previousValue);
		assertEquals(newValue, header.getAttribute("test"));
	}

	@Test
	public void testProperties() {
		MessageHeader header = new MessageHeader();
		String previousValue = header.setProperty("foo", "bar");
		assertNull(previousValue);
		assertEquals("bar", header.getProperty("foo"));
		assertNull(header.getProperty("nosuchproperty"));
		Set<String> names = header.getPropertyNames();
		assertEquals(1, names.size());
		assertTrue(names.contains("foo"));
		previousValue = header.setProperty("foo", "baz");
		assertEquals("bar", previousValue);
		assertEquals("baz", header.getProperty("foo"));
	}

	@Test
	public void testSetAttributeIfAbsent() {
		MessageHeader header = new MessageHeader();
		Integer integer = new Integer(123);
		assertNull(header.getAttribute("test"));
		assertFalse(header.getAttributeNames().contains("test"));
		Object existingValue = header.setAttributeIfAbsent("test", integer);
		assertNull(existingValue);
		assertEquals(integer, header.getAttribute("test"));
		assertTrue(header.getAttributeNames().contains("test"));
	}

	@Test
	public void testSetAttributeIfAbsentDoesNotOverride() {
		MessageHeader header = new MessageHeader();
		Integer originalValue = new Integer(123);
		header.setAttributeIfAbsent("test", originalValue);
		Object existingValue = header.setAttributeIfAbsent("test", new Integer(456));
		assertEquals(originalValue, header.getAttribute("test"));
		assertEquals(originalValue, existingValue);
	}

}
