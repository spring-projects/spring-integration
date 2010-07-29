/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.commons.serializer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.NotSerializableException;
import java.io.Serializable;

import org.junit.Test;


/**
 * @author Gary Russell
 * @since 2.0
 *
 */
public class JavaSerializationTests {

	@Test
	public void testGood() {
		SerializingConverter toBytes = new SerializingConverter();
		byte[] bytes = toBytes.convert("Testing");
		DeserializingConverter fromBytes = new DeserializingConverter();
		assertEquals("Testing", fromBytes.convert(bytes));
	}
	
	@Test
	public void testBadSerializeNotSerializable() {
		SerializingConverter toBytes = new SerializingConverter();
		try {
			toBytes.convert(new Object());
			fail("Expected IllegalArgumentException");
		} catch (SerializationFailureException e) {
			assertNotNull(e.getCause());
			assertTrue(e.getCause() instanceof IllegalArgumentException);
		}
		
	}

	@Test
	public void testBadSerializeNotSerializableField() {
		SerializingConverter toBytes = new SerializingConverter();
		try {
			toBytes.convert(new UnSerializable());
			fail("Expected SerializationFailureException");
		} catch (SerializationFailureException e) {
			assertNotNull(e.getCause());
			assertTrue(e.getCause() instanceof NotSerializableException);
		}
		
	}

	@Test
	public void testBadDeserialize() {
		DeserializingConverter fromBytes = new DeserializingConverter();
		try {
			fromBytes.convert("Junk".getBytes());
			fail("Expected DeserializationFailureException");
		} catch (DeserializationFailureException e) { }
	}

	class UnSerializable implements Serializable {
		private static final long serialVersionUID = 1L;
		@SuppressWarnings("unused")
		private Object object;
	}
}
