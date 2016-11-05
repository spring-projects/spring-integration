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

/**
 * Factory class to create TCP Serializer/Deserializers used to
 * encode/decode messages to/from a TCP stream.
 * This is used to simplify configuration with Java, such as
 *
 * <pre class="code">
 * TcpNetServerConnectionFactory server = new TcpNetServerConnectionFactory(1234);
 * server.setSerializer(TcpCodecs.lf());
 * server.setDserializer(TcpCodecs.lf());
 * ...
 * </pre>
 *
 * @author Gary Russell
 * @since 5.0
 *
 */
public final class TcpCodecs {

	private static ByteArrayLengthHeaderSerializer oneByteLHS;

	private static ByteArrayLengthHeaderSerializer twoByteLHS;

	private static ByteArrayLengthHeaderSerializer fourByteLHS;

	private TcpCodecs() {
		super();
	}

	/**
	 * @return a {@link ByteArrayCrLfSerializer}.
	 */
	public static ByteArrayCrLfSerializer crlf() {
		return ByteArrayCrLfSerializer.INSTANCE;
	}

	/**
	 * @return a {@link ByteArrayLfSerializer}.
	 */
	public static ByteArrayLfSerializer lf() {
		return ByteArrayLfSerializer.INSTANCE;
	}

	/**
	 * @return a {@link ByteArrayRawSerializer}.
	 */
	public static ByteArrayRawSerializer raw() {
		return ByteArrayRawSerializer.INSTANCE;
	}

	/**
	 * @return a {@link ByteArrayStxEtxSerializer}.
	 */
	public static ByteArrayStxEtxSerializer stxetx() {
		return ByteArrayStxEtxSerializer.INSTANCE;
	}

	/**
	 * @param terminator the terminator indicating message end.
	 * @return a {@link ByteArraySingleTerminatorSerializer} using the supplied
	 * terminator.
	 */
	public static ByteArraySingleTerminatorSerializer singleTerminator(byte terminator) {
		return new ByteArraySingleTerminatorSerializer(terminator);
	}

	/**
	 * @return a {@link ByteArrayLengthHeaderSerializer} with a 1 byte header.
	 */
	public static ByteArrayLengthHeaderSerializer lengthHeader1() {
		if (oneByteLHS == null) {
			oneByteLHS = new ByteArrayLengthHeaderSerializer(1);
		}
		return oneByteLHS;
	}

	/**
	 * @return a {@link ByteArrayLengthHeaderSerializer} with a 2 byte header.
	 */
	public static ByteArrayLengthHeaderSerializer lengthHeader2() {
		if (twoByteLHS == null) {
			twoByteLHS = new ByteArrayLengthHeaderSerializer(2);
		}
		return twoByteLHS;
	}

	/**
	 * @return a {@link ByteArrayLengthHeaderSerializer} with a 4 byte header.
	 */
	public static ByteArrayLengthHeaderSerializer lengthHeader4() {
		if (fourByteLHS == null) {
			fourByteLHS = new ByteArrayLengthHeaderSerializer(4);
		}
		return fourByteLHS;
	}

	/**
	 * @param bytes header length.
	 * @return a {@link ByteArrayLengthHeaderSerializer} with a 1, 2 or 4 byte header.
	 */
	public static ByteArrayLengthHeaderSerializer lengthHeader(int bytes) {
		switch (bytes) {
		case 1:
			return lengthHeader1();
		case 2:
			return lengthHeader2();
		case 4:
			return lengthHeader4();
		default:
			throw new IllegalArgumentException("Only 1, 2 or 4 byte headers are supported");
		}
	}

}
