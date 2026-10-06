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

package org.springframework.integration.http.converter;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.HashMap;

import org.junit.jupiter.api.Test;

import org.springframework.core.serializer.support.SerializationFailedException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * Tests for the deserialization allowlist of {@link SerializingHttpMessageConverter}.
 *
 * @author Uwez Khan
 * @author Artem Bilan
 * @author Hyun Lee
 * @author Glenn Renfro
 *
 * @since 5.5.22
 */
public class SerializingHttpMessageConverterTests {

	@Test
	public void readsAllowedClassWhenPatternMatches() throws Exception {
		SerializingHttpMessageConverter converter = new SerializingHttpMessageConverter("java.util.*");

		HashMap<String, String> payload = new HashMap<>();
		payload.put("testKey", "testValue");

		Serializable result = converter.readInternal(Serializable.class, message(serialize(payload)));

		assertThat(result).isEqualTo(payload);
	}

	@Test
	public void allowsBasicTypesEvenWithRestrictivePatterns() throws Exception {
		SerializingHttpMessageConverter converter = new SerializingHttpMessageConverter("com.example.*");

		Serializable result = converter.readInternal(Serializable.class, message(serialize("a String payload")));

		assertThat(result).isEqualTo("a String payload");
	}

	@Test
	public void rejectsClassNotOnAllowList() throws Exception {
		SerializingHttpMessageConverter converter = new SerializingHttpMessageConverter("com.example.*");

		byte[] body = serialize(new TestPayload());

		assertThatExceptionOfType(SerializationFailedException.class)
				.isThrownBy(() -> converter.readInternal(Serializable.class, message(body)))
				.withRootCauseInstanceOf(SecurityException.class);
	}

	@Test
	public void readsClassAddedToAllowList() throws Exception {
		SerializingHttpMessageConverter converter = new SerializingHttpMessageConverter("com.example.*");
		byte[] body = serialize(new TestPayload());
		assertThatExceptionOfType(SerializationFailedException.class)
				.isThrownBy(() -> converter.readInternal(Serializable.class, message(body)))
				.withRootCauseInstanceOf(SecurityException.class);

		converter.addAllowedPatterns(TestPayload.class.getName());

		Serializable result = converter.readInternal(Serializable.class, message(serialize(new TestPayload())));

		assertThat(result).isInstanceOf(TestPayload.class);
	}

	@Test
	public void requiresAllowedPatterns() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new SerializingHttpMessageConverter(new String[0]))
				.withMessage("'allowedPatterns' must not be empty");
	}

	@Test
	public void requiresNonNullAllowedPatterns() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new SerializingHttpMessageConverter((String[]) null))
				.withMessage("'allowedPatterns' must not be empty");
	}

	@Test
	public void rejectsInvalidAllowedPatternEntries() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new SerializingHttpMessageConverter("java.util.*", " "))
				.withMessageContaining("must not contain null, empty or whitespace-only patterns");
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new SerializingHttpMessageConverter("java.util.*", null))
				.withMessageContaining("must not contain null, empty or whitespace-only patterns");
	}

	@Test
	@SuppressWarnings("removal")
	public void patternsConstructorRejectsClearingOrInvalidMutation() throws Exception {
		SerializingHttpMessageConverter converter = new SerializingHttpMessageConverter("com.example.*");
		assertThatIllegalArgumentException()
				.isThrownBy(converter::setAllowedPatterns)
				.withMessageContaining("must not be empty");
		assertThatIllegalArgumentException()
				.isThrownBy(() -> converter.addAllowedPatterns(TestPayload.class.getName(), ""))
				.withMessageContaining("must not contain null, empty or whitespace-only patterns");

		byte[] body = serialize(new TestPayload());
		assertThatExceptionOfType(SerializationFailedException.class)
				.isThrownBy(() -> converter.readInternal(Serializable.class, message(body)))
				.withRootCauseInstanceOf(SecurityException.class);
	}

	@Test
	@SuppressWarnings("removal")
	public void legacyConstructorIsUnrestrictedUntilPatternsConfigured() throws Exception {
		SerializingHttpMessageConverter converter = new SerializingHttpMessageConverter();
		byte[] body = serialize(new TestPayload());
		assertThat(converter.readInternal(Serializable.class, message(body))).isInstanceOf(TestPayload.class);

		converter.setAllowedPatterns("com.example.*");
		assertThatExceptionOfType(SerializationFailedException.class)
				.isThrownBy(() -> converter.readInternal(Serializable.class, message(body)))
				.withRootCauseInstanceOf(SecurityException.class);

		assertThatIllegalArgumentException()
				.isThrownBy(converter::setAllowedPatterns)
				.withMessage("'allowedPatterns' must not be empty");
		assertThatExceptionOfType(SerializationFailedException.class)
				.isThrownBy(() -> converter.readInternal(Serializable.class, message(body)))
				.withRootCauseInstanceOf(SecurityException.class);
	}

	private static byte[] serialize(Serializable object) throws IOException {
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		try (ObjectOutputStream objectStream = new ObjectOutputStream(byteStream)) {
			objectStream.writeObject(object);
		}
		return byteStream.toByteArray();
	}

	private static HttpInputMessage message(byte[] body) {
		return new HttpInputMessage() {

			@Override
			public InputStream getBody() {
				return new ByteArrayInputStream(body);
			}

			@Override
			public HttpHeaders getHeaders() {
				return new HttpHeaders();
			}

		};
	}

	@SuppressWarnings("serial")
	private static final class TestPayload implements Serializable {

	}

}
