/*
 * Copyright 2002-2023 the original author or authors.
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

package org.springframework.integration.util;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.core.convert.converter.Converter;
import org.springframework.util.ClassUtils;

/**
 * Utility to help generate UUID instances from generic objects.
 *
 * @author Dave Syer
 * @author Gary Russell
 * @author Christian Tzolov
 */
public class UUIDConverter implements Converter<Object, UUID> {

	public static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;

	private static final Pattern UUID_REGEX = Pattern
			.compile("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

	/**
	 * Convert the input to a UUID using the convenience method {@link #getUUID(Object)}.
	 *
	 * @see org.springframework.core.convert.converter.Converter#convert(java.lang.Object)
	 */
	@Override
	public UUID convert(Object source) {
		return getUUID(source);
	}

	/**
	 * Convenient utility to convert an object to a UUID. If the input is
	 * <ul>
	 * <li>null: returns null</li>
	 * <li>a UUID: returns the input unchanged</li>
	 * <li>a String formatted as a UUID: returns the result of {@link UUID#fromString(String)}</li>
	 * <li>any other String: returns {@link UUID#nameUUIDFromBytes(byte[])} with bytes generated from the input</li>
	 * <li>a primitive or primitive wrapper: converts to a String ans then uses the previous conversion method</li>
	 * <li>Serializable: returns the {@link UUID#nameUUIDFromBytes(byte[])} with the serialized bytes of the input</li>
	 * </ul>
	 * If none of the above applies there will be an exception trying to serialize.
	 *
	 * @param input an Object
	 * @return a UUID constructed from the input
	 */
	public static UUID getUUID(Object input) {
		if (input == null) {
			return null;
		}
		if (input instanceof UUID) {
			return (UUID) input;
		}
		if (input instanceof String) {
			String inputText = (String) input;
			if (isValidUuidString(inputText)) {
				return UUID.fromString(inputText);
			}
			else {
				return UUID.nameUUIDFromBytes((inputText).getBytes(DEFAULT_CHARSET));
			}
		}
		if (ClassUtils.isPrimitiveOrWrapper(input.getClass())) {
			return UUID.nameUUIDFromBytes(input.toString().getBytes(DEFAULT_CHARSET));
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

	/**
	 * Verifies if the provided UUID string complies with the standard representation.
	 *
	 * @param uuid UUID text to verify.
	 * @return Returns true for string standard representation and false otherwise.
	 */
	private static boolean isValidUuidString(String uuid) {
		return UUID_REGEX.matcher(uuid).matches();
	}

}
