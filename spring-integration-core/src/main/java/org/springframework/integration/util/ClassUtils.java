/*
 * Copyright 2002-2019 the original author or authors.
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

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import org.springframework.lang.Nullable;
import org.springframework.util.ReflectionUtils;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 *
 * @since 2.0
 */
public abstract class ClassUtils {

	/**
	 * Map with primitive wrapper type as key and corresponding primitive
	 * type as value, for example: Integer.class -> int.class.
	 */
	private static final Map<Class<?>, Class<?>> PRIMITIVE_WRAPPER_TYPE_MAP = new HashMap<>(8);


	/**
	 * The {@link Function#apply(Object)} method object.
	 */
	public static final Method FUNCTION_APPLY_METHOD =
			ReflectionUtils.findMethod(Function.class, "apply", (Class<?>[]) null);

	/**
	 * The {@link Supplier#get()} method object.
	 */
	public static final Method SUPPLIER_GET_METHOD =
			ReflectionUtils.findMethod(Supplier.class, "get", (Class<?>[]) null);

	/**
	 * The {@code org.springframework.integration.core.GenericSelector#accept(Object)} method object.
	 */
	public static final Method SELECTOR_ACCEPT_METHOD;

	/**
	 * The {@code org.springframework.integration.transformer.GenericTransformer#transform(Object)} method object.
	 */
	public static final Method TRANSFORMER_TRANSFORM_METHOD;

	/**
	 * The {@code org.springframework.integration.handler.GenericHandler#handle(Object, Map)} method object.
	 */
	public static final Method HANDLER_HANDLE_METHOD;

	/**
	 * The {@code kotlin.jvm.functions.Function0} class object.
	 */
	public static final Class<?> KOTLIN_FUNCTION_0_CLASS;

	/**
	 * The {@code kotlin.jvm.functions.Function0#invoke} method object.
	 */
	public static final Method KOTLIN_FUNCTION_0_INVOKE_METHOD;

	/**
	 * The {@code kotlin.jvm.functions.Function1} class object.
	 */
	public static final Class<?> KOTLIN_FUNCTION_1_CLASS;

	/**
	 * The {@code kotlin.Unit} class object.
	 */
	public static final Class<?> KOTLIN_UNIT_CLASS;

	static {
		PRIMITIVE_WRAPPER_TYPE_MAP.put(Boolean.class, boolean.class);
		PRIMITIVE_WRAPPER_TYPE_MAP.put(Byte.class, byte.class);
		PRIMITIVE_WRAPPER_TYPE_MAP.put(Character.class, char.class);
		PRIMITIVE_WRAPPER_TYPE_MAP.put(Double.class, double.class);
		PRIMITIVE_WRAPPER_TYPE_MAP.put(Float.class, float.class);
		PRIMITIVE_WRAPPER_TYPE_MAP.put(Integer.class, int.class);
		PRIMITIVE_WRAPPER_TYPE_MAP.put(Long.class, long.class);
		PRIMITIVE_WRAPPER_TYPE_MAP.put(Short.class, short.class);

		Class<?> genericSelectorClass = null;
		try {
			genericSelectorClass =
					org.springframework.util.ClassUtils.forName(
							"org.springframework.integration.core.GenericSelector",
							org.springframework.util.ClassUtils.getDefaultClassLoader());
		}
		catch (ClassNotFoundException e) {
			ReflectionUtils.rethrowRuntimeException(e);
		}

		SELECTOR_ACCEPT_METHOD = ReflectionUtils.findMethod(genericSelectorClass, "accept", (Class<?>[]) null);

		Class<?> genericTransformerClass = null;
		try {
			genericTransformerClass =
					org.springframework.util.ClassUtils.forName(
							"org.springframework.integration.transformer.GenericTransformer",
							org.springframework.util.ClassUtils.getDefaultClassLoader());
		}
		catch (ClassNotFoundException e) {
			ReflectionUtils.rethrowRuntimeException(e);
		}

		TRANSFORMER_TRANSFORM_METHOD =
				ReflectionUtils.findMethod(genericTransformerClass, "transform", (Class<?>[]) null);

		Class<?> genericHandlerClass = null;
		try {
			genericHandlerClass =
					org.springframework.util.ClassUtils.forName(
							"org.springframework.integration.handler.GenericHandler",
							org.springframework.util.ClassUtils.getDefaultClassLoader());
		}
		catch (ClassNotFoundException e) {
			ReflectionUtils.rethrowRuntimeException(e);
		}

		HANDLER_HANDLE_METHOD = ReflectionUtils.findMethod(genericHandlerClass, "handle", (Class<?>[]) null);

		Class<?> kotlinClass = null;
		Method kotlinMethod = null;
		try {
			kotlinClass = org.springframework.util.ClassUtils.forName("kotlin.jvm.functions.Function0",
					org.springframework.util.ClassUtils.getDefaultClassLoader());

			kotlinMethod = ReflectionUtils.findMethod(kotlinClass, "invoke", (Class<?>[]) null);
		}
		catch (ClassNotFoundException e) {
			//Ignore: assume no Kotlin in classpath
		}
		finally {
			KOTLIN_FUNCTION_0_CLASS = kotlinClass;
			KOTLIN_FUNCTION_0_INVOKE_METHOD = kotlinMethod;
		}

		kotlinClass = null;
		kotlinMethod = null;
		try {
			kotlinClass = org.springframework.util.ClassUtils.forName("kotlin.jvm.functions.Function1",
					org.springframework.util.ClassUtils.getDefaultClassLoader());
		}
		catch (ClassNotFoundException e) {
			//Ignore: assume no Kotlin in classpath
		}
		finally {
			KOTLIN_FUNCTION_1_CLASS = kotlinClass;
		}

		kotlinClass = null;
		try {
			kotlinClass = org.springframework.util.ClassUtils.forName("kotlin.Unit",
					org.springframework.util.ClassUtils.getDefaultClassLoader());
		}
		catch (ClassNotFoundException e) {
			//Ignore: assume no Kotlin in classpath
		}
		finally {
			KOTLIN_UNIT_CLASS = kotlinClass;
		}
	}

	public static Class<?> findClosestMatch(Class<?> type, Set<Class<?>> candidates, boolean failOnTie) {
		int minTypeDiffWeight = Integer.MAX_VALUE;
		Class<?> closestMatch = null;
		for (Class<?> candidate : candidates) {
			int typeDiffWeight = getTypeDifferenceWeight(candidate, type);
			if (typeDiffWeight < minTypeDiffWeight) {
				minTypeDiffWeight = typeDiffWeight;
				closestMatch = candidate;
			}
			else if (failOnTie && typeDiffWeight < Integer.MAX_VALUE && (typeDiffWeight == minTypeDiffWeight)) {
				throw new IllegalStateException("Unresolvable ambiguity while attempting to find closest match for [" +
						type.getName() + "]. Candidate types [" + closestMatch.getName() + "] and [" +
						candidate.getName() + "] have equal weight.");
			}
		}
		return closestMatch;
	}

	private static int getTypeDifferenceWeight(Class<?> candidate, Class<?> type) {
		int result = 0;
		if (!org.springframework.util.ClassUtils.isAssignable(candidate, type)) {
			return Integer.MAX_VALUE;
		}
		Class<?> superClass = type.getSuperclass();
		while (superClass != null) {
			if (type.equals(superClass)) {
				result = result + 2;
				superClass = null;
			}
			else if (org.springframework.util.ClassUtils.isAssignable(candidate, superClass)) {
				result = result + 2;
				superClass = superClass.getSuperclass();
			}
			else {
				superClass = null;
			}
		}
		if (candidate.isInterface()) {
			result = result + 1;
		}
		return result;
	}

	/**
	 * Resolve the given class if it is a primitive wrapper class,
	 * returning the corresponding primitive type instead.
	 * @param clazz the wrapper class to check
	 * @return the corresponding primitive if the clazz is a wrapper, otherwise null
	 */
	@Nullable
	public static Class<?> resolvePrimitiveType(Class<?> clazz) {
		return PRIMITIVE_WRAPPER_TYPE_MAP.get(clazz);
	}

	/**
	 * Check if class is Java lambda.
	 * @param aClass the {@link Class} to check.
	 * @return true if class is a Java lambda.
	 * @since 5.2
	 */
	public static boolean isLambda(Class<?> aClass) {
		return aClass.isSynthetic() && !aClass.isAnonymousClass() && !aClass.isLocalClass();
	}

	/**
	 * Check if class is {@code kotlin.jvm.functions.Function0}.
	 * @param aClass the {@link Class} to check.
	 * @return true if class is a {@code kotlin.jvm.functions.Function0} implementation.
	 * @since 5.2
	 */
	public static boolean isKotlinFaction0(Class<?> aClass) {
		return KOTLIN_FUNCTION_0_CLASS != null && KOTLIN_FUNCTION_0_CLASS.isAssignableFrom(aClass);
	}

	/**
	 * Check if class is {@code kotlin.jvm.functions.Function1}.
	 * @param aClass the {@link Class} to check.
	 * @return true if class is a {@code kotlin.jvm.functions.Function1} implementation.
	 * @since 5.2
	 */
	public static boolean isKotlinFaction1(Class<?> aClass) {
		return KOTLIN_FUNCTION_1_CLASS != null && KOTLIN_FUNCTION_1_CLASS.isAssignableFrom(aClass);
	}

	/**
	 * Check if class is {@code kotlin.Unit}.
	 * @param aClass the {@link Class} to check.
	 * @return true if class is a {@code kotlin.Unit} implementation.
	 * @since 5.3.2
	 */
	public static boolean isKotlinUnit(Class<?> aClass) {
		return KOTLIN_UNIT_CLASS != null && KOTLIN_UNIT_CLASS.isAssignableFrom(aClass);
	}

}
