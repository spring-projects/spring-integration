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

import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import org.springframework.integration.test.util.TestUtils;

/**
 * @author Gary Russell
 * @since 5.0
 *
 */
public class TcpCodecsTests {

	@Test
	public void testAll() {
		AbstractByteArraySerializer codec = TcpCodecs.crlf();
		assertThat(codec, instanceOf(ByteArrayCrLfSerializer.class));
		codec = TcpCodecs.lf();
		assertThat(codec, instanceOf(ByteArrayLfSerializer.class));
		codec = TcpCodecs.raw();
		assertThat(codec, instanceOf(ByteArrayRawSerializer.class));
		codec = TcpCodecs.stxetx();
		assertThat(codec, instanceOf(ByteArrayStxEtxSerializer.class));
		codec = TcpCodecs.singleTerminator((byte) 23);
		assertThat(codec, instanceOf(ByteArraySingleTerminatorSerializer.class));
		assertEquals((byte) 23, TestUtils.getPropertyValue(codec, "terminator"));
		codec = TcpCodecs.lengthHeader1();
		assertThat(codec, instanceOf(ByteArrayLengthHeaderSerializer.class));
		assertEquals(1, TestUtils.getPropertyValue(codec, "headerSize"));
		codec = TcpCodecs.lengthHeader2();
		assertThat(codec, instanceOf(ByteArrayLengthHeaderSerializer.class));
		assertEquals(2, TestUtils.getPropertyValue(codec, "headerSize"));
		codec = TcpCodecs.lengthHeader4();
		assertThat(codec, instanceOf(ByteArrayLengthHeaderSerializer.class));
		assertEquals(4, TestUtils.getPropertyValue(codec, "headerSize"));
		codec = TcpCodecs.lengthHeader(1);
		assertThat(codec, instanceOf(ByteArrayLengthHeaderSerializer.class));
		assertEquals(1, TestUtils.getPropertyValue(codec, "headerSize"));
		codec = TcpCodecs.lengthHeader(2);
		assertThat(codec, instanceOf(ByteArrayLengthHeaderSerializer.class));
		assertEquals(2, TestUtils.getPropertyValue(codec, "headerSize"));
		codec = TcpCodecs.lengthHeader(4);
		assertThat(codec, instanceOf(ByteArrayLengthHeaderSerializer.class));
		assertEquals(4, TestUtils.getPropertyValue(codec, "headerSize"));
	}

}
