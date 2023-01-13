/*
 * Copyright 2016-2023 the original author or authors.
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

/**
 * Factory class to create TCP Serializer/Deserializers used to
 * encode/decode messages to/from a TCP stream.
 * This is used to simplify configuration with Java, such as
 *
 * <pre class="code">
 * TcpNetServerConnectionFactory server = new TcpNetServerConnectionFactory(1234);
 * server.setSerializer(TcpCodecs.lf());
 * server.setDeserializer(TcpCodecs.lf());
 * ...
 * </pre>
 *
 * @author Gary Russell
 *
 * @since 5.0
 *
 */
public final class TcpCodecs {

	private static ByteArrayLengthHeaderSerializer oneByteLHS;

	private static ByteArrayLengthHeaderSerializer twoByteLHS;

	private static ByteArrayLengthHeaderSerializer fourByteLHS;

	private TcpCodecs() {
	}

	/**
	 * Return a serializer with the default max message size for deserialization.
	 * @return a {@link ByteArrayCrLfSerializer}.
	 * @see AbstractByteArraySerializer#DEFAULT_MAX_MESSAGE_SIZE
	 */
	public static ByteArrayCrLfSerializer crlf() {
		return ByteArrayCrLfSerializer.INSTANCE;
	}

	/**
	 * Return a serializer with the default max message size for deserialization.
	 * {@value AbstractByteArraySerializer#DEFAULT_MAX_MESSAGE_SIZE}.
	 * @return a {@link ByteArrayLfSerializer}.
	 * @see AbstractByteArraySerializer#DEFAULT_MAX_MESSAGE_SIZE
	 */
	public static ByteArrayLfSerializer lf() {
		return ByteArrayLfSerializer.INSTANCE;
	}

	/**
	 * Return a serializer with the default max message size for deserialization.
	 * @return a {@link ByteArrayRawSerializer}.
	 * @see AbstractByteArraySerializer#DEFAULT_MAX_MESSAGE_SIZE
	 */
	public static ByteArrayRawSerializer raw() {
		return ByteArrayRawSerializer.INSTANCE;
	}

	/**
	 * Return a serializer with the default max message size for deserialization.
	 * @return a {@link ByteArrayStxEtxSerializer}.
	 * @see AbstractByteArraySerializer#DEFAULT_MAX_MESSAGE_SIZE
	 */
	public static ByteArrayStxEtxSerializer stxetx() {
		return ByteArrayStxEtxSerializer.INSTANCE;
	}

	/**
	 * Return a serializer with the default max message size for deserialization.
	 * @param terminator the terminator indicating message end.
	 * @return a {@link ByteArraySingleTerminatorSerializer} using the supplied
	 * terminator.
	 * @see AbstractByteArraySerializer#DEFAULT_MAX_MESSAGE_SIZE
	 */
	public static ByteArraySingleTerminatorSerializer singleTerminator(byte terminator) {
		return new ByteArraySingleTerminatorSerializer(terminator);
	}

	/**
	 * Return a serializer with the default max message size for deserialization.
	 * @return a {@link ByteArrayLengthHeaderSerializer} with a 1 byte header.
	 * @see AbstractByteArraySerializer#DEFAULT_MAX_MESSAGE_SIZE
	 */
	public static ByteArrayLengthHeaderSerializer lengthHeader1() {
		if (oneByteLHS == null) {
			oneByteLHS = new ByteArrayLengthHeaderSerializer(ByteArrayLengthHeaderSerializer.HEADER_SIZE_UNSIGNED_BYTE);
		}
		return oneByteLHS;
	}

	/**
	 * Return a serializer with the default max message size for deserialization.
	 * @return a {@link ByteArrayLengthHeaderSerializer} with a 2 byte header.
	 * @see AbstractByteArraySerializer#DEFAULT_MAX_MESSAGE_SIZE
	 */
	public static ByteArrayLengthHeaderSerializer lengthHeader2() {
		if (twoByteLHS == null) {
			twoByteLHS = new ByteArrayLengthHeaderSerializer(
					ByteArrayLengthHeaderSerializer.HEADER_SIZE_UNSIGNED_SHORT);
		}
		return twoByteLHS;
	}

	/**
	 * Return a serializer with the default max message size for deserialization.
	 * @return a {@link ByteArrayLengthHeaderSerializer} with a 4 byte header.
	 * @see AbstractByteArraySerializer#DEFAULT_MAX_MESSAGE_SIZE
	 */
	public static ByteArrayLengthHeaderSerializer lengthHeader4() {
		if (fourByteLHS == null) {
			fourByteLHS = new ByteArrayLengthHeaderSerializer(ByteArrayLengthHeaderSerializer.HEADER_SIZE_INT);
		}
		return fourByteLHS;
	}

	/**
	 * Return a serializer with the default max message size for deserialization.
	 * @param bytes header length.
	 * @return a {@link ByteArrayLengthHeaderSerializer} with a 1, 2 or 4 byte header.
	 * @see AbstractByteArraySerializer#DEFAULT_MAX_MESSAGE_SIZE
	 */
	public static ByteArrayLengthHeaderSerializer lengthHeader(int bytes) {
		return switch (bytes) {
			case ByteArrayLengthHeaderSerializer.HEADER_SIZE_UNSIGNED_BYTE -> lengthHeader1();
			case ByteArrayLengthHeaderSerializer.HEADER_SIZE_UNSIGNED_SHORT -> lengthHeader2();
			case ByteArrayLengthHeaderSerializer.HEADER_SIZE_INT -> lengthHeader4();
			default -> throw new IllegalArgumentException("Only 1, 2 or 4 byte headers are supported");
		};
	}

	/**
	 * Return a serializer with the provided max message size for deserialization.
	 * @param maxMessageSize the max message size.
	 * @return a {@link ByteArrayCrLfSerializer}.
	 * @since 5.1.3
	 */
	public static ByteArrayCrLfSerializer crlf(int maxMessageSize) {
		ByteArrayCrLfSerializer codec = new ByteArrayCrLfSerializer();
		codec.setMaxMessageSize(maxMessageSize);
		return codec;
	}

	/**
	 * Return a serializer with the provided max message size for deserialization.
	 * @param maxMessageSize the max message size.
	 * @return a {@link ByteArrayLfSerializer}.
	 * @since 5.1.3
	 */
	public static ByteArrayLfSerializer lf(int maxMessageSize) {
		ByteArrayLfSerializer codec = new ByteArrayLfSerializer();
		codec.setMaxMessageSize(maxMessageSize);
		return codec;
	}

	/**
	 * Return a serializer with the provided max message size for deserialization.
	 * @param maxMessageSize the max message size.
	 * @return a {@link ByteArrayRawSerializer}.
	 * @since 5.1.3
	 */
	public static ByteArrayRawSerializer raw(int maxMessageSize) {
		ByteArrayRawSerializer codec = new ByteArrayRawSerializer();
		codec.setMaxMessageSize(maxMessageSize);
		return codec;
	}

	/**
	 * Return a serializer with the provided max message size for deserialization.
	 * @param maxMessageSize the max message size.
	 * @return a {@link ByteArrayStxEtxSerializer}.
	 * @since 5.1.3
	 */
	public static ByteArrayStxEtxSerializer stxetx(int maxMessageSize) {
		ByteArrayStxEtxSerializer codec = new ByteArrayStxEtxSerializer();
		codec.setMaxMessageSize(maxMessageSize);
		return codec;
	}

	/**
	 * Return a serializer with the provided max message size for deserialization.
	 * @param terminator the terminator indicating message end.
	 * @param maxMessageSize the max message size.
	 * @return a {@link ByteArraySingleTerminatorSerializer} using the supplied
	 * terminator.
	 * @since 5.1.3
	 */
	public static ByteArraySingleTerminatorSerializer singleTerminator(byte terminator, int maxMessageSize) {
		ByteArraySingleTerminatorSerializer codec = new ByteArraySingleTerminatorSerializer(terminator);
		codec.setMaxMessageSize(maxMessageSize);
		return codec;
	}

	/**
	 * Return a serializer with the provided max message size for deserialization.
	 * @param maxMessageSize the max message size.
	 * @return a {@link ByteArrayLengthHeaderSerializer} with a 1 byte header.
	 * @since 5.1.3
	 */
	public static ByteArrayLengthHeaderSerializer lengthHeader1(int maxMessageSize) {
		ByteArrayLengthHeaderSerializer codec = new ByteArrayLengthHeaderSerializer(
				ByteArrayLengthHeaderSerializer.HEADER_SIZE_UNSIGNED_BYTE);
		codec.setMaxMessageSize(maxMessageSize);
		return codec;
	}

	/**
	 * Return a serializer with the provided max message size for deserialization.
	 * @param maxMessageSize the max message size.
	 * @return a {@link ByteArrayLengthHeaderSerializer} with a 2 byte header.
	 * @since 5.1.3
	 */
	public static ByteArrayLengthHeaderSerializer lengthHeader2(int maxMessageSize) {
		ByteArrayLengthHeaderSerializer codec = new ByteArrayLengthHeaderSerializer(
				ByteArrayLengthHeaderSerializer.HEADER_SIZE_UNSIGNED_SHORT);
		codec.setMaxMessageSize(maxMessageSize);
		return codec;
	}

	/**
	 * Return a serializer with the provided max message size for deserialization.
	 * @param maxMessageSize the max message size.
	 * @return a {@link ByteArrayLengthHeaderSerializer} with a 4 byte header.
	 * @since 5.1.3
	 */
	public static ByteArrayLengthHeaderSerializer lengthHeader4(int maxMessageSize) {
		ByteArrayLengthHeaderSerializer codec = new ByteArrayLengthHeaderSerializer(
				ByteArrayLengthHeaderSerializer.HEADER_SIZE_INT);
		codec.setMaxMessageSize(maxMessageSize);
		return codec;
	}

}
