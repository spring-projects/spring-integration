/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.integration.ip.tcp.serializer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Set;

import org.junit.Test;

import org.springframework.integration.test.util.TestUtils;

/**
 * @author Gary Russell
 * @since 4.3
 *
 */
public class PooledDeserializationTests {

	@Test
	public void testCRLF() throws IOException {
		ByteArrayCrLfSerializer deser = new ByteArrayCrLfSerializer();
		deser.setPoolSize(2);
		ByteArrayInputStream bais = new ByteArrayInputStream("foo\r\n".getBytes());
		for (int i = 0; i < 5; i++) {
			bais.reset();
			byte[] bytes = deser.deserialize(bais);
			assertEquals("foo", new String(bytes));
		}
		try {
			deser.deserialize(bais);
			fail("Expected SoftEndOfStreamException");
		}
		catch (SoftEndOfStreamException e) {
			// expected
		}
		assertEquals(1, TestUtils.getPropertyValue(deser, "pool.allocated", Set.class).size());
		assertEquals(0, TestUtils.getPropertyValue(deser, "pool.inUse", Set.class).size());
	}

	@Test
	public void testRawMaxMessageSizeEqualDontReturnPooledItem() throws IOException {
		ByteArrayRawSerializer deser = new ByteArrayRawSerializer();
		deser.setPoolSize(2);
		deser.setMaxMessageSize(3);
		ByteArrayInputStream bais = new ByteArrayInputStream("foo".getBytes());
		byte[] bytes = deser.deserialize(bais);
		assertEquals("foo", new String(bytes));
		assertEquals(1, TestUtils.getPropertyValue(deser, "pool.allocated", Set.class).size());
		assertEquals(0, TestUtils.getPropertyValue(deser, "pool.inUse", Set.class).size());
		assertNotSame(bytes, TestUtils.getPropertyValue(deser, "pool.allocated", Set.class).iterator().next());
	}

}
