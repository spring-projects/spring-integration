/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.integration.util;

import java.util.Date;
import java.util.UUID;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

/**
 * @author Dave Syer
 * @author Gary Russell
 *
 */
public class UUIDConverterTests {

	@Test
	public void testConvertNull() {
		assertThat(UUIDConverter.getUUID(null)).isNull();
	}

	@Test
	public void testConvertUUID() {
		UUID uuid = UUID.randomUUID();
		assertThat(UUIDConverter.getUUID(uuid)).isEqualTo(uuid);
	}

	@Test
	public void testConvertUUIDString() {
		UUID uuid = UUID.randomUUID();
		assertThat(UUIDConverter.getUUID(uuid.toString())).isEqualTo(uuid);
	}

	@Test
	public void testConvertAlmostUUIDString() {
		String name = "1-2-3-4";
		try {
			UUID.fromString(name);
			fail("IllegalArgumentException expected");
		}
		catch (IllegalArgumentException e) {
			String message = e.getMessage();
			assertThat(message.contains("Invalid UUID string")).as("Wrong message: " + message).isTrue();
		}
		assertThat(UUIDConverter.getUUID(name)).isNotNull();
	}

	@Test
	public void testConvertRandomString() {
		UUID uuid = UUIDConverter.getUUID("foo");
		assertThat(uuid).isNotNull();
		String uuidString = uuid.toString();
		assertThat(UUIDConverter.getUUID("foo").toString()).isEqualTo(uuidString);
		assertThat(UUIDConverter.getUUID(uuid).toString()).isEqualTo(uuidString);
	}

	@Test
	public void testConvertPrimitive() {
		assertThat(UUIDConverter.getUUID(1L)).isNotNull();
	}

	@Test
	public void testConvertSerializable() {
		assertThat(UUIDConverter.getUUID(new Date())).isNotNull();
	}

	@Test(expected = IllegalArgumentException.class)
	public void testConvertNonSerializable() {
		assertThat(UUIDConverter.getUUID(new Object())).isNotNull();
	}

}
