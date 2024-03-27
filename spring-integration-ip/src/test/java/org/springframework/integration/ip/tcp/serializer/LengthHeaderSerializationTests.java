/*
 * Copyright 2002-2024 the original author or authors.
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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

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
	public void testInt() throws IOException {
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
		ByteArrayInputStream bisBad = new ByteArrayInputStream(bytes);
		assertThatIllegalArgumentException().isThrownBy(() -> serializer.deserialize(bisBad));
	}

	@Test
	public void testByte() throws IOException {
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
		assertThatIllegalArgumentException().isThrownBy(() -> serializer.serialize(test255.getBytes(), bos));
	}

	@Test
	public void testShort1() throws IOException {
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
	public void testShort2() throws IOException {
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
		assertThatIllegalArgumentException().isThrownBy(() -> serializer.serialize(testFFFF.getBytes(), bos));
	}

	@Test
	public void testBad() {
		assertThatIllegalArgumentException().isThrownBy(() -> new ByteArrayLengthHeaderSerializer(23));
	}

	@Test
	public void testIntInclusive() throws IOException {
		ByteArrayLengthHeaderSerializer serializer = new ByteArrayLengthHeaderSerializer().inclusive();
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		serializer.serialize(TEST.getBytes(), bos);
		byte[] bytes = bos.toByteArray();
		assertThat(bytes[0]).isEqualTo((byte) 0);
		assertThat(bytes[1]).isEqualTo((byte) 0);
		assertThat(bytes[2]).isEqualTo((byte) 0);
		assertThat(bytes[3]).isEqualTo((byte) (TEST.length() + 4));
		ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
		bytes = serializer.deserialize(bis);
		assertThat(new String(bytes)).isEqualTo(TEST);
	}

	@Test
	public void testByteInclusive() throws IOException {
		ByteArrayLengthHeaderSerializer serializer = new ByteArrayLengthHeaderSerializer(
				ByteArrayLengthHeaderSerializer.HEADER_SIZE_UNSIGNED_BYTE).inclusive();
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		serializer.serialize(TEST.getBytes(), bos);
		byte[] bytes = bos.toByteArray();
		assertThat(bytes[0] & 0xff).isEqualTo((byte) (TEST.length() + 1));
		ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
		bytes = serializer.deserialize(bis);
		assertThat(new String(bytes)).isEqualTo(TEST);
		assertThatIllegalArgumentException().isThrownBy(() -> serializer.serialize(test255.getBytes(), bos));
	}

	@Test
	public void testShort1Inclusive() throws IOException {
		ByteArrayLengthHeaderSerializer serializer = new ByteArrayLengthHeaderSerializer(
				ByteArrayLengthHeaderSerializer.HEADER_SIZE_UNSIGNED_SHORT).inclusive();
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		serializer.serialize(TEST.getBytes(), bos);
		byte[] bytes = bos.toByteArray();
		assertThat(bytes[0]).isEqualTo((byte) 0);
		assertThat(bytes[1] & 0xff).isEqualTo(TEST.length() + 2);
		ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
		bytes = serializer.deserialize(bis);
		assertThat(new String(bytes)).isEqualTo(TEST);
	}

	@Test
	public void testShort2Inclusive() {
		AbstractByteArraySerializer serializer = new ByteArrayLengthHeaderSerializer(
				ByteArrayLengthHeaderSerializer.HEADER_SIZE_UNSIGNED_SHORT).inclusive();
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		assertThatIllegalArgumentException().isThrownBy(() -> serializer.serialize(testFFFF.getBytes(), bos));
	}

}
