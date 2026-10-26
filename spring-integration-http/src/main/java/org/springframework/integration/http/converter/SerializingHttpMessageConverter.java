/*
 * Copyright 2002-present the original author or authors.
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
 * By default, only the {@link AllowListDeserializingConverter#DEFAULT_ALLOWED_PATTERNS} are allowed.
 * Use {@link #addAllowedPatterns(String...)} to trust additional classes/packages,
 * or {@link #setAllowedPatterns(String...)} to replace the defaults.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 * @author Uwez Khan
 * @author Hyun Lee
 * @author Glenn Renfro
 *
 * @since 2.0
 */
public class SerializingHttpMessageConverter extends AbstractHttpMessageConverter<Serializable> {

	private static final MediaType APPLICATION_JAVA_SERIALIZED_OBJECT =
			new MediaType("application", "x-java-serialized-object");

	private final AllowListDeserializingConverter deserializingConverter = new AllowListDeserializingConverter();

	/**
	 * Create a new instance which allows only the
	 * {@link AllowListDeserializingConverter#DEFAULT_ALLOWED_PATTERNS} for deserialization.
	 */
	public SerializingHttpMessageConverter() {
		super(APPLICATION_JAVA_SERIALIZED_OBJECT);
	}

	/**
	 * Set simple patterns for allowable packages/classes for deserialization.
	 * The patterns will be applied in order until a match is found.
	 * A class can be fully qualified, or a wildcard {@code '*'} is allowed at the
	 * beginning or end of the class name.
	 * Examples: {@code com.example.*}, {@code *.MyClass}.
	 * The provided patterns replace the existing ones, including the
	 * {@link AllowListDeserializingConverter#DEFAULT_ALLOWED_PATTERNS}.
	 * Primitives, {@link String} and arrays of allowed types are always allowed.
	 * @param allowedPatterns the patterns.
	 * @since 5.5.22
	 */
	public void setAllowedPatterns(String... allowedPatterns) {
		this.deserializingConverter.setAllowedPatterns(allowedPatterns);
	}

	/**
	 * Add package/class patterns to the allowed list, in addition to the existing ones
	 * (by default the {@link AllowListDeserializingConverter#DEFAULT_ALLOWED_PATTERNS}).
	 * @param allowedPatterns the patterns to add.
	 * @since 5.5.22
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
