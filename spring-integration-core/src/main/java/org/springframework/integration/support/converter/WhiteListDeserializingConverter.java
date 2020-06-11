/*
 * Copyright 2002-2020 the original author or authors.
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

import org.springframework.core.convert.converter.Converter;
import org.springframework.core.serializer.DefaultDeserializer;
import org.springframework.core.serializer.Deserializer;

/**
 * A {@link Converter} that delegates to a
 * {@link org.springframework.core.serializer.Deserializer} to convert data in a byte
 * array to an object. By default, if using a {@link DefaultDeserializer} all
 * classes/packages are deserialized. If you receive data from untrusted sources, consider
 * adding trusted classes/packages using {@link #setWhiteListPatterns(String...)} or
 * {@link #addWhiteListPatterns(String...)}.
 *
 * @author Gary Russell
 * @author Mark Fisher
 * @author Juergen Hoeller
 *
 * @since 4.2.13
 *
 * @deprecated since 5.4 in favor of AllowListDeserializingConverter
 */
@Deprecated
public class WhiteListDeserializingConverter extends AllowListDeserializingConverter {

	/**
	 * Create a {@code WhiteListDeserializingConverter} with default
	 * {@link java.io.ObjectInputStream} configuration, using the "latest user-defined
	 * ClassLoader".
	 */
	public WhiteListDeserializingConverter() {
		super();
	}

	/**
	 * Create a {@code WhiteListDeserializingConverter} for using an
	 * {@link java.io.ObjectInputStream} with the given {@code ClassLoader}.
	 * @param classLoader the class loader to use for deserialization.
	 */
	public WhiteListDeserializingConverter(ClassLoader classLoader) {
		super(classLoader);
	}

	/**
	 * Create a {@code WhiteListDeserializingConverter} that delegates to the provided
	 * {@link Deserializer}.
	 * @param deserializer the deserializer to use.
	 */
	public WhiteListDeserializingConverter(Deserializer<Object> deserializer) {
		super(deserializer);
	}

	/**
	 * Set simple patterns for allowable packages/classes for deserialization.
	 * The patterns will be applied in order until a match is found.
	 * A class can be fully qualified or a wildcard '*' is allowed at the
	 * beginning or end of the class name.
	 * Examples: {@code com.foo.*}, {@code *.MyClass}.
	 * @param whiteListPatterns the patterns.
	 */
	public void setWhiteListPatterns(String... whiteListPatterns) {
		setAllowedPatterns(whiteListPatterns);
	}

	/**
	 * Add package/class patterns to the white list.
	 * @param patterns the patterns to add.
	 * @see #setWhiteListPatterns(String...)
	 */
	public void addWhiteListPatterns(String... patterns) {
		addAllowedPatterns(patterns);
	}

	protected void checkWhiteList(Class<?> clazz) {
		checkAllowList(clazz);
	}

}
