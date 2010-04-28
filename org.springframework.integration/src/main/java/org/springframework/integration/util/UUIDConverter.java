/*
 * Copyright 2002-2008 the original author or authors.
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
package org.springframework.integration.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.UnsupportedEncodingException;
import java.util.UUID;

import org.springframework.core.convert.converter.Converter;
import org.springframework.util.ClassUtils;

/**
 * Utility to help generate UUID instances from generic objects.
 * 
 * @author Dave Syer
 *
 */
public class UUIDConverter implements Converter<Object, UUID> {

	public static final String DEFAULT_CHARSET = "UTF-8";
	private final String charset;
	
	public UUIDConverter() {
		this(DEFAULT_CHARSET);
	}
	
	public UUIDConverter(String charset) {
		this.charset = charset;
	}


	public UUID convert(Object source) {
		return getUUID(source, charset);
	}

	public static UUID getUUID(Object input) {
		return getUUID(input, DEFAULT_CHARSET);
	}
	
	public static UUID getUUID(Object input, String charset) {

		if (input == null) {
			return null;
		}

		if (input instanceof UUID) {
			return (UUID) input;
		}

		if (input instanceof String) {
			try {
				return UUID.fromString((String) input);
			}
			catch (IllegalArgumentException e) {
				try {
					return UUID.nameUUIDFromBytes(((String) input).getBytes(charset));
				}
				catch (UnsupportedEncodingException ex) {
					throw new IllegalStateException("Cannot convert String using charset=" + charset, ex);
				}
			}
		}

		if (ClassUtils.isPrimitiveOrWrapper(input.getClass())) {
			try {
				return UUID.nameUUIDFromBytes(input.toString().getBytes(charset));
			}
			catch (UnsupportedEncodingException e) {
				throw new IllegalStateException("Cannot convert primitive using charset=" + charset, e);
			}
		}

		byte[] bytes = serialize(input);
		return UUID.nameUUIDFromBytes(bytes);

	}

	private static byte[] serialize(Object object) {

		if (object == null) {
			return null;
		}

		ByteArrayOutputStream stream = new ByteArrayOutputStream();
		try {
			new ObjectOutputStream(stream).writeObject(object);
		}
		catch (IOException e) {
			throw new IllegalArgumentException("Could not serialize object of type: " + object.getClass(), e);
		}

		return stream.toByteArray();

	}

}
