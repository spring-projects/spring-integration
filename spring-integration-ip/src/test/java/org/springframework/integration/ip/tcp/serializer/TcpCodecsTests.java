/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.ip.tcp.serializer;

import org.junit.Test;

import org.springframework.integration.test.util.TestUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
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
		assertThat(TestUtils.getPropertyValue(codec, "terminator")).isEqualTo((byte) 23);
		codec = TcpCodecs.lengthHeader1();
		assertThat(codec).isInstanceOf(ByteArrayLengthHeaderSerializer.class);
		assertThat(TestUtils.getPropertyValue(codec, "headerSize")).isEqualTo(1);
		codec = TcpCodecs.lengthHeader2();
		assertThat(codec).isInstanceOf(ByteArrayLengthHeaderSerializer.class);
		assertThat(TestUtils.getPropertyValue(codec, "headerSize")).isEqualTo(2);
		codec = TcpCodecs.lengthHeader4();
		assertThat(codec).isInstanceOf(ByteArrayLengthHeaderSerializer.class);
		assertThat(TestUtils.getPropertyValue(codec, "headerSize")).isEqualTo(4);
		codec = TcpCodecs.lengthHeader(1);
		assertThat(codec).isInstanceOf(ByteArrayLengthHeaderSerializer.class);
		assertThat(TestUtils.getPropertyValue(codec, "headerSize")).isEqualTo(1);
		codec = TcpCodecs.lengthHeader(2);
		assertThat(codec).isInstanceOf(ByteArrayLengthHeaderSerializer.class);
		assertThat(TestUtils.getPropertyValue(codec, "headerSize")).isEqualTo(2);
		codec = TcpCodecs.lengthHeader(4);
		assertThat(codec).isInstanceOf(ByteArrayLengthHeaderSerializer.class);
		assertThat(TestUtils.getPropertyValue(codec, "headerSize")).isEqualTo(4);
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
		assertThat(TestUtils.getPropertyValue(codec, "terminator")).isEqualTo((byte) 23);
		codec = TcpCodecs.lengthHeader1(123);
		assertThat(codec).isInstanceOf(ByteArrayLengthHeaderSerializer.class);
		assertThat(codec.getMaxMessageSize()).isEqualTo(123);
		assertThat(TestUtils.getPropertyValue(codec, "headerSize")).isEqualTo(1);
		codec = TcpCodecs.lengthHeader2(123);
		assertThat(codec).isInstanceOf(ByteArrayLengthHeaderSerializer.class);
		assertThat(codec.getMaxMessageSize()).isEqualTo(123);
		assertThat(TestUtils.getPropertyValue(codec, "headerSize")).isEqualTo(2);
		codec = TcpCodecs.lengthHeader4(123);
		assertThat(codec).isInstanceOf(ByteArrayLengthHeaderSerializer.class);
		assertThat(codec.getMaxMessageSize()).isEqualTo(123);
		assertThat(TestUtils.getPropertyValue(codec, "headerSize")).isEqualTo(4);
	}

}
