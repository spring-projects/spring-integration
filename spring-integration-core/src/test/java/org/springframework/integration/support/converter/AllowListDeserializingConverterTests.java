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

package org.springframework.integration.support.converter;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.junit.jupiter.api.Test;

import org.springframework.core.serializer.DefaultDeserializer;
import org.springframework.core.serializer.Deserializer;
import org.springframework.core.serializer.support.SerializationFailedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

/**
 * @author Glenn Renfro
 *
 * @since 7.2.0
 */
class AllowListDeserializingConverterTests {

	static final String EMPTY_MESSAGE = "'allowedPatterns' must not be empty";

	static final String INVALID_ENTRY_MESSAGE =
			"'allowedPatterns' must not contain null, empty or whitespace-only patterns";

	private static final String TRUSTED_BEAN = TrustedBean.class.getName();

	@Test
	@SuppressWarnings("deprecation")
	void legacyNoArgConstructorIsUnrestricted() throws IOException {
		AllowListDeserializingConverter converter = new AllowListDeserializingConverter();
		assertThat(converter.convert(serialize(new TrustedBean("test")))).isInstanceOf(TrustedBean.class);
	}

	@Test
	@SuppressWarnings("deprecation")
	void legacyClassLoaderConstructorIsUnrestricted() throws IOException {
		AllowListDeserializingConverter converter =
				new AllowListDeserializingConverter(getClass().getClassLoader());
		assertThat(converter.convert(serialize(new TrustedBean("test")))).isInstanceOf(TrustedBean.class);
	}

	@Test
	@SuppressWarnings("deprecation")
	void legacyConstructorRejectsInvalidMutations() throws IOException {
		AllowListDeserializingConverter converter = new AllowListDeserializingConverter();
		converter.setAllowedPatterns("com.example.*");
		assertUnauthorized(converter, serialize(new TrustedBean("test")));
		assertThatIllegalArgumentException()
				.isThrownBy(converter::setAllowedPatterns)
				.withMessage(EMPTY_MESSAGE);
		assertThatIllegalArgumentException()
				.isThrownBy(() -> converter.addAllowedPatterns(TRUSTED_BEAN, " "))
				.withMessage(INVALID_ENTRY_MESSAGE);
		assertUnauthorized(converter, serialize(new TrustedBean("test")));
	}

	@Test
	@SuppressWarnings("deprecation")
	void legacyAddAllowedPatternsExtendsAllowList() throws IOException {
		AllowListDeserializingConverter converter = new AllowListDeserializingConverter();
		converter.addAllowedPatterns("com.example.*");
		assertUnauthorized(converter, serialize(new TrustedBean("test")));
		converter.addAllowedPatterns(TRUSTED_BEAN);
		assertThat(converter.convert(serialize(new TrustedBean("test")))).isInstanceOf(TrustedBean.class);
	}

	@Test
	void patternsConstructorEnforcesExactMatch() throws IOException {
		AllowListDeserializingConverter converter = new AllowListDeserializingConverter(TRUSTED_BEAN);
		assertThat(converter.convert(serialize(new TrustedBean("test"))))
				.isInstanceOf(TrustedBean.class)
				.extracting("name")
				.isEqualTo("test");
		assertUnauthorized(converter, serialize(new DeclinedBean()));
	}

	@Test
	void classLoaderPatternsConstructorEnforcesWildcardMatch() throws IOException {
		AllowListDeserializingConverter converter =
				new AllowListDeserializingConverter(getClass().getClassLoader(), "*$TrustedBean");
		assertThat(converter.convert(serialize(new TrustedBean("test")))).isInstanceOf(TrustedBean.class);
		assertUnauthorized(converter, serialize(new DeclinedBean()));
	}

	@Test
	void deserializerPatternsConstructorChecksResultClass() throws IOException {
		AllowListDeserializingConverter converter =
				new AllowListDeserializingConverter(new DefaultDeserializer(), "org.springframework.*");
		assertThat(converter.convert(serialize(new TrustedBean("test")))).isInstanceOf(TrustedBean.class);

		Deserializer<Object> customDeserializer = inputStream -> new DeclinedBean();
		AllowListDeserializingConverter customConverter =
				new AllowListDeserializingConverter(customDeserializer, TRUSTED_BEAN);
		assertThatExceptionOfType(SerializationFailedException.class)
				.isThrownBy(() -> customConverter.convert(new byte[0]))
				.withCauseInstanceOf(SecurityException.class);
	}

	@Test
	void explicitWildcardAcceptsAnyClass() throws IOException {
		AllowListDeserializingConverter converter = new AllowListDeserializingConverter("*");
		assertThat(converter.convert(serialize(new DeclinedBean()))).isInstanceOf(DeclinedBean.class);
	}

	@Test
	@SuppressWarnings("deprecation")
	void addAllowedPatternsAllowsPreviouslyRejectedClass() throws IOException {
		AllowListDeserializingConverter converter = new AllowListDeserializingConverter(TRUSTED_BEAN);
		assertUnauthorized(converter, serialize(new DeclinedBean()));
		converter.addAllowedPatterns(DeclinedBean.class.getName());
		assertThat(converter.convert(serialize(new DeclinedBean()))).isInstanceOf(DeclinedBean.class);
		assertThat(converter.convert(serialize(new TrustedBean("test")))).isInstanceOf(TrustedBean.class);
	}

	@Test
	@SuppressWarnings("deprecation")
	void setAllowedPatternsReplacesPatterns() throws IOException {
		AllowListDeserializingConverter converter = new AllowListDeserializingConverter(TRUSTED_BEAN);
		converter.setAllowedPatterns(DeclinedBean.class.getName());
		assertThat(converter.convert(serialize(new DeclinedBean()))).isInstanceOf(DeclinedBean.class);
		assertUnauthorized(converter, serialize(new TrustedBean("test")));
	}

	@Test
	@SuppressWarnings("deprecation")
	void withDeserializerPreservesPatternsAndConstructionMode() throws IOException {
		AllowListDeserializingConverter converter = new AllowListDeserializingConverter(TRUSTED_BEAN);
		AllowListDeserializingConverter replaced =
				converter.withDeserializer(new DefaultDeserializer(getClass().getClassLoader()));
		assertThat(replaced.convert(serialize(new TrustedBean("test")))).isInstanceOf(TrustedBean.class);
		assertUnauthorized(replaced, serialize(new DeclinedBean()));
		assertThatIllegalArgumentException()
				.isThrownBy(replaced::setAllowedPatterns)
				.withMessage(EMPTY_MESSAGE);
	}

	@Test
	@SuppressWarnings("deprecation")
	void legacyWithDeserializerCopiesPatterns() throws IOException {
		AllowListDeserializingConverter converter = new AllowListDeserializingConverter();
		AllowListDeserializingConverter replaced = converter.withDeserializer(new DefaultDeserializer());
		assertThat(replaced.convert(serialize(new DeclinedBean()))).isInstanceOf(DeclinedBean.class);
		converter.setAllowedPatterns(TRUSTED_BEAN);
		replaced = converter.withDeserializer(new DefaultDeserializer());
		assertUnauthorized(replaced, serialize(new DeclinedBean()));
	}

	static byte[] serialize(Object object) throws IOException {
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		try (ObjectOutputStream objectStream = new ObjectOutputStream(byteStream)) {
			objectStream.writeObject(object);
		}
		return byteStream.toByteArray();
	}

	static void assertUnauthorized(AllowListDeserializingConverter converter, byte[] bytes) {
		assertThatExceptionOfType(SerializationFailedException.class)
				.isThrownBy(() -> converter.convert(bytes))
				.havingRootCause()
				.isInstanceOf(SecurityException.class)
				.withMessageStartingWith("Attempt to deserialize unauthorized");
	}

	@SuppressWarnings("serial")
	static class TrustedBean implements Serializable {

		final String name;

		TrustedBean(String name) {
			this.name = name;
		}

	}

	@SuppressWarnings("serial")
	static class DeclinedBean implements Serializable {

	}

}
