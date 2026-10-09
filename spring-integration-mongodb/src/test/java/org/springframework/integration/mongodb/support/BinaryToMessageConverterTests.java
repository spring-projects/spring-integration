/*
 * Copyright 2026-present the original author or authors.
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

package org.springframework.integration.mongodb.support;

import java.io.Serializable;

import org.bson.types.Binary;
import org.junit.jupiter.api.Test;

import org.springframework.core.serializer.support.SerializationFailedException;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Glenn Renfro
 *
 * @since 7.2.0
 */
class BinaryToMessageConverterTests {

	private static final String[] MESSAGE_PATTERNS = {
			"org.springframework.messaging.support.GenericMessage",
			"org.springframework.messaging.MessageHeaders",
			"java.util.UUID",
			"java.util.HashMap"
	};

	private final MessageToBinaryConverter messageToBinaryConverter = new MessageToBinaryConverter();

	@Test
	@SuppressWarnings("deprecation")
	void legacyConstructorIsUnrestricted() {
		BinaryToMessageConverter converter = new BinaryToMessageConverter();
		assertThat(converter.convert(toBinary(new UntrustedPayload())).getPayload())
				.isInstanceOf(UntrustedPayload.class);
	}

	@Test
	void patternsConstructorEnforcesAllowList() {
		BinaryToMessageConverter converter = new BinaryToMessageConverter(withPayloadPattern());
		assertThat(converter.convert(toBinary(new TrustedPayload())).getPayload())
				.isInstanceOf(TrustedPayload.class);
		assertUnauthorized(converter);
	}

	@Test
	void patternsConstructorRejectsInvalidPatterns() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new BinaryToMessageConverter((String[]) null))
				.withMessage("'allowedPatterns' must not be empty");
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new BinaryToMessageConverter(new String[0]))
				.withMessage("'allowedPatterns' must not be empty");
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new BinaryToMessageConverter("java.util.*", " "))
				.withMessageContaining("whitespace-only");
	}

	@Test
	@SuppressWarnings("deprecation")
	void addAllowedPatternsExtendsPatternsAndRejectsInvalid() {
		BinaryToMessageConverter converter = new BinaryToMessageConverter(withPayloadPattern());
		assertUnauthorized(converter);
		assertThatIllegalArgumentException().isThrownBy(converter::addAllowedPatterns);
		assertUnauthorized(converter);
		converter.addAllowedPatterns(UntrustedPayload.class.getName());
		assertThat(converter.convert(toBinary(new UntrustedPayload())).getPayload())
				.isInstanceOf(UntrustedPayload.class);
	}

	private Binary toBinary(Object payload) {
		Binary binary = this.messageToBinaryConverter.convert(new GenericMessage<>(payload));
		assertThat(binary).isNotNull();
		return binary;
	}

	private void assertUnauthorized(BinaryToMessageConverter converter) {
		Binary binary = toBinary(new UntrustedPayload());
		assertThatExceptionOfType(SerializationFailedException.class)
				.isThrownBy(() -> converter.convert(binary))
				.withCauseInstanceOf(SecurityException.class);
	}

	private static String[] withPayloadPattern() {
		String[] patterns = new String[MESSAGE_PATTERNS.length + 1];
		System.arraycopy(MESSAGE_PATTERNS, 0, patterns, 0, MESSAGE_PATTERNS.length);
		patterns[MESSAGE_PATTERNS.length] = TrustedPayload.class.getName();
		return patterns;
	}

	@SuppressWarnings("serial")
	private static final class TrustedPayload implements Serializable {

	}

	@SuppressWarnings("serial")
	private static final class UntrustedPayload implements Serializable {

	}

}
