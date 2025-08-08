/*
 * Copyright © 2013 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2013-present the original author or authors.
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
