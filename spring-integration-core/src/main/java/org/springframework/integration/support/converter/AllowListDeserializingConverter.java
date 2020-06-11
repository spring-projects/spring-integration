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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.core.ConfigurableObjectInputStream;
import org.springframework.core.NestedIOException;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.serializer.DefaultDeserializer;
import org.springframework.core.serializer.Deserializer;
import org.springframework.core.serializer.support.SerializationFailedException;
import org.springframework.util.Assert;
import org.springframework.util.PatternMatchUtils;

/**
 * A {@link Converter} that delegates to a
 * {@link Deserializer} to convert data in a byte
 * array to an object. By default, if using a {@link DefaultDeserializer} all
 * classes/packages are deserialized. If you receive data from untrusted sources, consider
 * adding trusted classes/packages using {@link #setAllowedPatterns(String...)} or
 * {@link #addAllowedPatterns(String...)}.
 *
 * @author Gary Russell
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Artem Bilan
 *
 * @since 5.4
 */
public class AllowListDeserializingConverter implements Converter<byte[], Object> {

	private final Deserializer<Object> deserializer;

	private final ClassLoader defaultDeserializerClassLoader;

	private final boolean usingDefaultDeserializer;

	private final Set<String> allowedPatterns = new LinkedHashSet<>();


	/**
	 * Create a {@link AllowListDeserializingConverter} with default
	 * {@link ObjectInputStream} configuration, using the "latest user-defined
	 * ClassLoader".
	 */
	public AllowListDeserializingConverter() {
		this(new DefaultDeserializer());
	}

	/**
	 * Create a {@link AllowListDeserializingConverter} for using an
	 * {@link ObjectInputStream} with the given {@code ClassLoader}.
	 * @param classLoader the class loader to use for deserialization.
	 */
	public AllowListDeserializingConverter(ClassLoader classLoader) {
		this(new DefaultDeserializer(classLoader));
	}

	/**
	 * Create a {@link AllowListDeserializingConverter} that delegates to the provided
	 * {@link Deserializer}.
	 * @param deserializer the deserializer to use.
	 */
	public AllowListDeserializingConverter(Deserializer<Object> deserializer) {
		Assert.notNull(deserializer, "Deserializer must not be null");
		this.deserializer = deserializer;
		if (deserializer instanceof DefaultDeserializer) {
			ClassLoader classLoader = null;
			try {
				classLoader = (ClassLoader) new DirectFieldAccessor(deserializer).getPropertyValue("classLoader");
			}
			catch (Exception e) {
				// no-op
			}
			this.defaultDeserializerClassLoader = classLoader;
			this.usingDefaultDeserializer = true;
		}
		else {
			this.defaultDeserializerClassLoader = null;
			this.usingDefaultDeserializer = false;
		}
	}

	/**
	 * Set simple patterns for allowable packages/classes for deserialization.
	 * The patterns will be applied in order until a match is found.
	 * A class can be fully qualified or a wildcard '*' is allowed at the
	 * beginning or end of the class name.
	 * Examples: {@code com.foo.*}, {@code *.MyClass}.
	 * @param allowedPatterns the patterns.
	 */
	public void setAllowedPatterns(String... allowedPatterns) {
		this.allowedPatterns.clear();
		Collections.addAll(this.allowedPatterns, allowedPatterns);
	}

	/**
	 * Add package/class patterns to the allow list.
	 * @param patterns the patterns to add.
	 * @see #setAllowedPatterns(String...)
	 */
	public void addAllowedPatterns(String... patterns) {
		Collections.addAll(this.allowedPatterns, patterns);
	}

	@Override
	public Object convert(byte[] source) {
		ByteArrayInputStream byteStream = new ByteArrayInputStream(source);
		try {
			if (this.usingDefaultDeserializer) {
				return deserialize(byteStream);
			}
			else {
				return this.deserializer.deserialize(byteStream);
			}
		}
		catch (Exception ex) {
			throw new SerializationFailedException("Failed to deserialize payload. " +
					"Is the byte array a result of corresponding serialization for " +
					this.deserializer.getClass().getSimpleName() + "?", ex);
		}
	}

	protected Object deserialize(ByteArrayInputStream inputStream) throws IOException {
		try {
			ObjectInputStream objectInputStream = new ConfigurableObjectInputStream(inputStream,
					this.defaultDeserializerClassLoader) {

				@Override
				protected Class<?> resolveClass(ObjectStreamClass classDesc)
						throws IOException, ClassNotFoundException {
					Class<?> clazz = super.resolveClass(classDesc);
					checkAllowList(clazz);
					return clazz;
				}

			};
			return objectInputStream.readObject();
		}
		catch (ClassNotFoundException ex) {
			throw new NestedIOException("Failed to deserialize object type", ex);
		}
	}

	protected void checkAllowList(Class<?> clazz) {
		if (this.allowedPatterns.isEmpty()) {
			return;
		}
		if (clazz.isArray() || clazz.isPrimitive() || clazz.equals(String.class)
				|| Number.class.isAssignableFrom(clazz)) {
			return;
		}
		String className = clazz.getName();
		for (String pattern : this.allowedPatterns) {
			if (PatternMatchUtils.simpleMatch(pattern, className)) {
				return;
			}
		}
		throw new SecurityException("Attempt to deserialize unauthorized " + clazz);
	}

}
