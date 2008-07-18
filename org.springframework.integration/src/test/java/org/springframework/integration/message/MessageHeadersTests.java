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

package org.springframework.integration.message;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

/**
 * @author Mark Fisher
 */
public class MessageHeadersTests {

	@Test
	public void testTimestamp() {
		MessageHeaders headers = new MessageHeaders(null);
		assertNotNull(headers.getTimestamp());
	}

	@Test
	public void testNonTypedAccessOfHeaderValue() {
		Integer value = new Integer(123);
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("test", value);
		MessageHeaders headers = new MessageHeaders(map);
		assertEquals(value, headers.get("test"));
	}

	@Test
	public void testTypedAccessOfHeaderValue() {
		Integer value = new Integer(123);
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("test", value);
		MessageHeaders headers = new MessageHeaders(map);
		assertEquals(value, headers.get("test", Integer.class));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testHeaderValueAccessWithIncorrectType() {
		Integer value = new Integer(123);
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("test", value);
		MessageHeaders headers = new MessageHeaders(map);
		assertEquals(value, headers.get("test", String.class));
	}

	@Test
	public void testNullHeaderValue() {
		Map<String, Object> map = new HashMap<String, Object>();
		MessageHeaders headers = new MessageHeaders(map);
		assertNull(headers.get("nosuchattribute"));
	}

	@Test
	public void testNullHeaderValueWithTypedAccess() {
		Map<String, Object> map = new HashMap<String, Object>();
		MessageHeaders headers = new MessageHeaders(map);
		assertNull(headers.get("nosuchattribute", String.class));
	}

	@Test
	public void testHeaderKeys() {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("key1", "val1");
		map.put("key2", new Integer(123));
		MessageHeaders headers = new MessageHeaders(map);
		Set<String> keys = headers.keySet();
		assertTrue(keys.contains("key1"));
		assertTrue(keys.contains("key2"));
	}

}
