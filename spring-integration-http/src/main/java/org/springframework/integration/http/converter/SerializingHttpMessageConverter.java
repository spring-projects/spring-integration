/*
 * Copyright 2002-2024 the original author or authors.
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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.util.FileCopyUtils;

/**
 * An {@link org.springframework.http.converter.HttpMessageConverter} implementation for
 * {@link Serializable} instances.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class SerializingHttpMessageConverter extends AbstractHttpMessageConverter<Serializable> {

	private static final MediaType APPLICATION_JAVA_SERIALIZED_OBJECT =
			new MediaType("application", "x-java-serialized-object");

	/** Creates a new instance of the {@code SerializingHttpMessageConverter}. */
	public SerializingHttpMessageConverter() {
		super(APPLICATION_JAVA_SERIALIZED_OBJECT);
	}

	@Override
	public boolean supports(Class<?> clazz) {
		return Serializable.class.isAssignableFrom(clazz);
	}

	@Override
	public boolean canWrite(Class<?> clazz, MediaType mediaType) {
		return Serializable.class.isAssignableFrom(clazz) && canWrite(mediaType);
	}

	@Override
	@SuppressWarnings("rawtypes")
	public Serializable readInternal(Class clazz, HttpInputMessage inputMessage) throws IOException {
		try {
			return (Serializable) new ObjectInputStream(inputMessage.getBody()).readObject(); //NOSONAR
		}
		catch (ClassNotFoundException e) {
			throw new IllegalArgumentException(e);
		}
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
