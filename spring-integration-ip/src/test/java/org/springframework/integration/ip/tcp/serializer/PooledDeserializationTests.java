/*
 * Copyright 2016-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.ip.tcp.serializer;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Set;

import org.junit.jupiter.api.Test;

import org.springframework.integration.test.util.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Gary Russell
 * @author Glenn Renfro
 *
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
			assertThat(new String(bytes)).isEqualTo("foo");
		}
		try {
			deser.deserialize(bais);
			fail("Expected SoftEndOfStreamException");
		}
		catch (SoftEndOfStreamException e) {
			// expected
		}
		assertThat(TestUtils.<Set<?>>getPropertyValue(deser, "pool.allocated").size())
				.isEqualTo(1);
		assertThat(TestUtils.<Set<?>>getPropertyValue(deser, "pool.inUse")).isEmpty();
	}

	@Test
	public void testRawMaxMessageSizeEqualDontReturnPooledItem() throws IOException {
		ByteArrayRawSerializer deser = new ByteArrayRawSerializer();
		deser.setPoolSize(2);
		deser.setMaxMessageSize(3);
		ByteArrayInputStream bais = new ByteArrayInputStream("foo".getBytes());
		byte[] bytes = deser.deserialize(bais);
		assertThat(new String(bytes)).isEqualTo("foo");
		assertThat(TestUtils.<Set<?>>getPropertyValue(deser, "pool.allocated").size())
				.isEqualTo(1);
		assertThat(TestUtils.<Set<?>>getPropertyValue(deser, "pool.inUse")).isEmpty();
		assertThat(TestUtils.<Set<?>>getPropertyValue(deser, "pool.allocated").iterator().next())
				.isNotSameAs(bytes);
	}

}
