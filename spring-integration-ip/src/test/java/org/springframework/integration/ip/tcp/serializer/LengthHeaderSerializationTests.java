/*
 * Copyright 2002-2019 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;



/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0.4
 *
 */
public class LengthHeaderSerializationTests {

	private static final String TEST = "Test";
	private String test255;
	private String testFFFF;

	@Before
	public void setup() {
		char[] chars = new char[255];
		Arrays.fill(chars, 'x');
		test255 = new String(chars);
		chars = new char[0xffff];
		Arrays.fill(chars, 'x');
		testFFFF = new String(chars);
	}

	@Test
	public void testInt() throws Exception {
		AbstractByteArraySerializer serializer = new ByteArrayLengthHeaderSerializer();
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		serializer.serialize(TEST.getBytes(), bos);
		byte[] bytes = bos.toByteArray();
		assertThat(bytes[0]).isEqualTo((byte) 0);
		assertThat(bytes[1]).isEqualTo((byte) 0);
		assertThat(bytes[2]).isEqualTo((byte) 0);
		assertThat(bytes[3]).isEqualTo((byte) TEST.length());
		ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
		bytes = serializer.deserialize(bis);
		assertThat(new String(bytes)).isEqualTo(TEST);
		bytes[0] = -1;
		bis = new ByteArrayInputStream(bytes);
		try {
			bytes = serializer.deserialize(bis);
			fail("Expected negative length");
		}
		catch (IllegalArgumentException e) { }
	}

	@Test
	public void testByte() throws Exception {
		AbstractByteArraySerializer serializer = new ByteArrayLengthHeaderSerializer(
				ByteArrayLengthHeaderSerializer.HEADER_SIZE_UNSIGNED_BYTE);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		serializer.serialize(test255.getBytes(), bos);
		byte[] bytes = bos.toByteArray();
		assertThat(bytes[0] & 0xff).isEqualTo(test255.length());
		ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
		bytes = serializer.deserialize(bis);
		assertThat(new String(bytes)).isEqualTo(test255);
		test255 += "x";
		try {
			serializer.serialize(test255.getBytes(), bos);
			fail("Expected overflow");
		}
		catch (IllegalArgumentException e) { }
	}

	@Test
	public void testShort1() throws Exception {
		AbstractByteArraySerializer serializer = new ByteArrayLengthHeaderSerializer(
				ByteArrayLengthHeaderSerializer.HEADER_SIZE_UNSIGNED_SHORT);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		serializer.serialize(test255.getBytes(), bos);
		byte[] bytes = bos.toByteArray();
		assertThat(bytes[0]).isEqualTo((byte) 0);
		assertThat(bytes[1] & 0xff).isEqualTo(test255.length());
		ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
		bytes = serializer.deserialize(bis);
		assertThat(new String(bytes)).isEqualTo(test255);
	}

	@Test
	public void testShort2() throws Exception {
		AbstractByteArraySerializer serializer = new ByteArrayLengthHeaderSerializer(
				ByteArrayLengthHeaderSerializer.HEADER_SIZE_UNSIGNED_SHORT);
		serializer.setMaxMessageSize(0x10000);
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		serializer.serialize(testFFFF.getBytes(), bos);
		byte[] bytes = bos.toByteArray();
		assertThat(bytes[0] & 0xff).isEqualTo(0xff);
		assertThat(bytes[1] & 0xff).isEqualTo(0xff);
		ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
		bytes = serializer.deserialize(bis);
		assertThat(new String(bytes)).isEqualTo(testFFFF);
		testFFFF += "x";
		try {
			serializer.serialize(testFFFF.getBytes(), bos);
			fail("Expected overflow");
		}
		catch (IllegalArgumentException e) { }
	}

	@Test
	public void testBad() throws Exception {
		try {
			new ByteArrayLengthHeaderSerializer(23);
			fail("Expected illegal argument exception");
		}
		catch (IllegalArgumentException e) { }

	}
}
