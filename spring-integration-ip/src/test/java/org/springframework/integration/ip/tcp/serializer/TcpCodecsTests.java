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

import org.junit.jupiter.api.Test;

import org.springframework.integration.test.util.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @author Glenn Renfro
 *
 * @since 5.0
 *
 */
public class TcpCodecsTests {

	@Test
	public void testAll() {
		AbstractByteArraySerializer codec = TcpCodecs.crlf();
		assertThat(codec).isInstanceOf(ByteArrayCrLfSerializer.class);
		codec = TcpCodecs.lf();
		assertThat(codec).isInstanceOf(ByteArrayLfSerializer.class);
		codec = TcpCodecs.raw();
		assertThat(codec).isInstanceOf(ByteArrayRawSerializer.class);
		codec = TcpCodecs.stxetx();
		assertThat(codec).isInstanceOf(ByteArrayStxEtxSerializer.class);
		codec = TcpCodecs.singleTerminator((byte) 23);
		assertThat(codec).isInstanceOf(ByteArraySingleTerminatorSerializer.class);
		assertThat(TestUtils.<Byte>getPropertyValue(codec, "terminator")).isEqualTo((byte) 23);
		codec = TcpCodecs.lengthHeader1();
		assertThat(codec).isInstanceOf(ByteArrayLengthHeaderSerializer.class);
		assertThat(TestUtils.<Integer>getPropertyValue(codec, "headerSize")).isEqualTo(1);
		codec = TcpCodecs.lengthHeader2();
		assertThat(codec).isInstanceOf(ByteArrayLengthHeaderSerializer.class);
		assertThat(TestUtils.<Integer>getPropertyValue(codec, "headerSize")).isEqualTo(2);
		codec = TcpCodecs.lengthHeader4();
		assertThat(codec).isInstanceOf(ByteArrayLengthHeaderSerializer.class);
		assertThat(TestUtils.<Integer>getPropertyValue(codec, "headerSize")).isEqualTo(4);
		codec = TcpCodecs.lengthHeader(1);
		assertThat(codec).isInstanceOf(ByteArrayLengthHeaderSerializer.class);
		assertThat(TestUtils.<Integer>getPropertyValue(codec, "headerSize")).isEqualTo(1);
		codec = TcpCodecs.lengthHeader(2);
		assertThat(codec).isInstanceOf(ByteArrayLengthHeaderSerializer.class);
		assertThat(TestUtils.<Integer>getPropertyValue(codec, "headerSize")).isEqualTo(2);
		codec = TcpCodecs.lengthHeader(4);
		assertThat(codec).isInstanceOf(ByteArrayLengthHeaderSerializer.class);
		assertThat(TestUtils.<Integer>getPropertyValue(codec, "headerSize")).isEqualTo(4);
	}

	@Test
	public void testMaxLengths() {
		AbstractByteArraySerializer codec = TcpCodecs.crlf(123);
		assertThat(codec).isInstanceOf(ByteArrayCrLfSerializer.class);
		assertThat(codec.getMaxMessageSize()).isEqualTo(123);
		codec = TcpCodecs.lf(123);
		assertThat(codec).isInstanceOf(ByteArrayLfSerializer.class);
		assertThat(codec.getMaxMessageSize()).isEqualTo(123);
		codec = TcpCodecs.raw(123);
		assertThat(codec).isInstanceOf(ByteArrayRawSerializer.class);
		assertThat(codec.getMaxMessageSize()).isEqualTo(123);
		codec = TcpCodecs.stxetx(123);
		assertThat(codec).isInstanceOf(ByteArrayStxEtxSerializer.class);
		assertThat(codec.getMaxMessageSize()).isEqualTo(123);
		codec = TcpCodecs.singleTerminator((byte) 23, 123);
		assertThat(codec).isInstanceOf(ByteArraySingleTerminatorSerializer.class);
		assertThat(codec.getMaxMessageSize()).isEqualTo(123);
		assertThat(TestUtils.<Byte>getPropertyValue(codec, "terminator")).isEqualTo((byte) 23);
		codec = TcpCodecs.lengthHeader1(123);
		assertThat(codec).isInstanceOf(ByteArrayLengthHeaderSerializer.class);
		assertThat(codec.getMaxMessageSize()).isEqualTo(123);
		assertThat(TestUtils.<Integer>getPropertyValue(codec, "headerSize")).isEqualTo(1);
		codec = TcpCodecs.lengthHeader2(123);
		assertThat(codec).isInstanceOf(ByteArrayLengthHeaderSerializer.class);
		assertThat(codec.getMaxMessageSize()).isEqualTo(123);
		assertThat(TestUtils.<Integer>getPropertyValue(codec, "headerSize")).isEqualTo(2);
		codec = TcpCodecs.lengthHeader4(123);
		assertThat(codec).isInstanceOf(ByteArrayLengthHeaderSerializer.class);
		assertThat(codec.getMaxMessageSize()).isEqualTo(123);
		assertThat(TestUtils.<Integer>getPropertyValue(codec, "headerSize")).isEqualTo(4);
	}

}
