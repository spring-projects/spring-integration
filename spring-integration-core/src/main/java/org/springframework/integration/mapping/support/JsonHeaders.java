/*
 * Copyright 2013-2020 the original author or authors.
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

package org.springframework.integration.mapping.support;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.springframework.core.ResolvableType;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.lang.Nullable;
import org.springframework.util.ClassUtils;

/**
 * Pre-defined names and prefixes to be used for setting and/or retrieving JSON
 * entries from/to Message Headers and other adapter, e.g. AMQP.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 3.0
 */
public final class JsonHeaders {

	private JsonHeaders() {
	}

	public static final String PREFIX = "json";

	public static final String TYPE_ID = PREFIX + "__TypeId__";

	public static final String CONTENT_TYPE_ID = PREFIX + "__ContentTypeId__";

	public static final String KEY_TYPE_ID = PREFIX + "__KeyTypeId__";

	/**
	 * The header to represent a {@link ResolvableType}
	 * for the target deserialized object.
	 * @since 5.2
	 */
	public static final String RESOLVABLE_TYPE = PREFIX + "_resolvableType";

	public static final Collection<String> HEADERS =
			Collections.unmodifiableList(Arrays.asList(TYPE_ID, CONTENT_TYPE_ID, KEY_TYPE_ID, RESOLVABLE_TYPE));

	/**
	 * Build a {@link ResolvableType} for provided class components.
	 * @param classLoader a {@link ClassLoader} t load classes for components if needed.
	 * @param targetClassValue the class representation object.
	 * @param contentClassValue the collection element (or map value) class representation object.
	 * @param keyClassValue the map key class representation object.
	 * @return the {@link ResolvableType} based on provided class components
	 * @since 5.2.4
	 */
	public static ResolvableType buildResolvableType(ClassLoader classLoader, Object targetClassValue,
			@Nullable Object contentClassValue, @Nullable Object keyClassValue) {

		Class<?> targetClass = getClassForValue(classLoader, targetClassValue);
		Class<?> keyClass = getClassForValue(classLoader, keyClassValue);
		Class<?> contentClass = getClassForValue(classLoader, contentClassValue);

		return buildResolvableType(targetClass, contentClass, keyClass);
	}

	@Nullable
	private static Class<?> getClassForValue(ClassLoader classLoader, @Nullable Object classValue) {
		if (classValue instanceof Class<?>) {
			return (Class<?>) classValue;
		}
		else if (classValue != null) {
			try {
				return ClassUtils.forName(classValue.toString(), classLoader);
			}
			catch (ClassNotFoundException | LinkageError e) {
				throw new IllegalStateException(e);
			}
		}
		else {
			return null;
		}
	}

	/**
	 * Build a {@link ResolvableType} for provided class components.
	 * @param targetClass the class to use.
	 * @param contentClass the collection element (or map value) class.
	 * @param keyClass the map key class.
	 * @return the {@link ResolvableType} based on provided class components
	 * @since 5.2.4
	 */
	public static ResolvableType buildResolvableType(Class<?> targetClass, @Nullable Class<?> contentClass,
			@Nullable Class<?> keyClass) {

		if (keyClass != null) {
			return TypeDescriptor
					.map(targetClass,
							TypeDescriptor.valueOf(keyClass),
							TypeDescriptor.valueOf(contentClass))
					.getResolvableType();
		}
		else if (contentClass != null) {
			return TypeDescriptor
					.collection(targetClass,
							TypeDescriptor.valueOf(contentClass))
					.getResolvableType();
		}
		else {
			return ResolvableType.forClass(targetClass);
		}
	}

}
