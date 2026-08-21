/*
 * Copyright 2002-present the original author or authors.
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.jspecify.annotations.Nullable;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.integration.support.converter.AllowListDeserializingConverter;
import org.springframework.util.FileCopyUtils;

/**
 * An {@link org.springframework.http.converter.HttpMessageConverter} implementation for
 * {@link Serializable} instances.
 * <p>
 * Incoming requests are deserialized through an {@link AllowListDeserializingConverter}.
 * For backward compatibility no class restriction is applied by default; when this
 * converter is used to read requests from untrusted sources, configure an allowlist of
 * trusted classes/packages via {@link #setAllowedPatterns(String...)} or
 * {@link #addAllowedPatterns(String...)} to guard against unsafe Java deserialization.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 * @author Uwez Khan
 *
 * @since 2.0
 */
public class SerializingHttpMessageConverter extends AbstractHttpMessageConverter<Serializable> {

	private static final MediaType APPLICATION_JAVA_SERIALIZED_OBJECT =
			new MediaType("application", "x-java-serialized-object");

	private final AllowListDeserializingConverter deserializingConverter = new AllowListDeserializingConverter();

	/**
	 * Creates a new instance of the {@code SerializingHttpMessageConverter}.
	 */
	public SerializingHttpMessageConverter() {
		super(APPLICATION_JAVA_SERIALIZED_OBJECT);
	}

	/**
	 * Set simple patterns for allowable packages/classes for deserialization.
	 * The patterns will be applied in order until a match is found.
	 * A class can be fully qualified, or a wildcard {@code '*'} is allowed at the
	 * beginning or end of the class name.
	 * Examples: {@code com.foo.*}, {@code *.MyClass}.
	 * The basic types ({@link String}, {@link Number}, arrays and primitives) are always
	 * allowed. When no patterns are configured, all classes are deserialized (the previous,
	 * unrestricted behavior).
	 * @param allowedPatterns the patterns.
	 * @since 5.5.22
	 */
	public void setAllowedPatterns(String... allowedPatterns) {
		this.deserializingConverter.setAllowedPatterns(allowedPatterns);
	}

	/**
	 * Add package/class patterns to the allowed list.
	 * @param allowedPatterns the patterns to add.
	 * @since 5.5.22
	 * @see #setAllowedPatterns(String...)
	 */
	public void addAllowedPatterns(String... allowedPatterns) {
		this.deserializingConverter.addAllowedPatterns(allowedPatterns);
	}

	@Override
	public boolean supports(Class<?> clazz) {
		return Serializable.class.isAssignableFrom(clazz);
	}

	@Override
	public boolean canWrite(Class<?> clazz, @Nullable MediaType mediaType) {
		return Serializable.class.isAssignableFrom(clazz) && canWrite(mediaType);
	}

	@Override
	public Serializable readInternal(Class<? extends Serializable> clazz, HttpInputMessage inputMessage)
			throws IOException {

		byte[] body = FileCopyUtils.copyToByteArray(inputMessage.getBody());
		return (Serializable) this.deserializingConverter.convert(body);
	}

	@Override
	protected void writeInternal(Serializable object, HttpOutputMessage outputMessage) throws IOException {
		ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
		ObjectOutputStream objectStream = new ObjectOutputStream(byteStream);
		objectStream.writeObject(object);
		objectStream.flush();
		objectStream.close();
		byte[] bytes = byteStream.toByteArray();
		FileCopyUtils.copy(bytes, outputMessage.getBody());
	}

}
