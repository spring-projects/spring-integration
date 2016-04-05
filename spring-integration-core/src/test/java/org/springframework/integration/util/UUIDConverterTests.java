/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.integration.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Date;
import java.util.UUID;

import org.junit.Test;

/**
 * @author Dave Syer
 * @author Gary Russell
 *
 */
public class UUIDConverterTests {

	@Test
	public void testConvertNull() throws Exception {
		assertNull(UUIDConverter.getUUID(null));
	}

	@Test
	public void testConvertUUID() throws Exception {
		UUID uuid = UUID.randomUUID();
		assertEquals(uuid, UUIDConverter.getUUID(uuid));
	}

	@Test
	public void testConvertUUIDString() throws Exception {
		UUID uuid = UUID.randomUUID();
		assertEquals(uuid, UUIDConverter.getUUID(uuid.toString()));
	}

	@Test
	public void testConvertAlmostUUIDString() throws Exception {
		String name = "1-2-3-4";
		try {
			UUID.fromString(name);
			fail();
		}
		catch (IllegalArgumentException e) {
			String message = e.getMessage();
			assertTrue("Wrong message: " + message, message.contains("Invalid UUID string"));
		}
		assertNotNull(UUIDConverter.getUUID(name));
	}

	@Test
	public void testConvertRandomString() throws Exception {
		UUID uuid = UUIDConverter.getUUID("foo");
		assertNotNull(uuid);
		String uuidString = uuid.toString();
		assertEquals(uuidString, UUIDConverter.getUUID("foo").toString());
		assertEquals(uuidString, UUIDConverter.getUUID(uuid).toString());
	}

	@Test
	public void testConvertPrimitive() throws Exception {
		assertNotNull(UUIDConverter.getUUID(1L));
	}

	@Test
	public void testConvertSerializable() throws Exception {
		assertNotNull(UUIDConverter.getUUID(new Date()));
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConvertNonSerializable() throws Exception {
		assertNotNull(UUIDConverter.getUUID(new Object()));
	}

}
