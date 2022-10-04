/*
 * Copyright 2022 the original author or authors.
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

import org.springframework.core.KotlinDetector;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

import reactor.core.publisher.Mono;

/**
 * Additional utilities for working with Kotlin Coroutines.
 *
 * @author Artem Bilan
 *
 * @since 6.0
 *
 * @see org.springframework.core.CoroutinesUtils
 */
public final class CoroutinesUtils {

	/**
	 * The {@link kotlin.coroutines.Continuation} class object.
	 */
	@Nullable
	public static final Class<?> KOTLIN_CONTINUATION_CLASS;

	static {
		if (KotlinDetector.isKotlinPresent()) {
			Class<?> kotlinClass = null;
			try {
				kotlinClass = ClassUtils.forName("kotlin.coroutines.Continuation", ClassUtils.getDefaultClassLoader());
			}
			catch (ClassNotFoundException ex) {
				//Ignore: assume no Kotlin in classpath
			}
			finally {
				KOTLIN_CONTINUATION_CLASS = kotlinClass;
			}
		}
		else {
			KOTLIN_CONTINUATION_CLASS = null;
		}
	}

	public static boolean isContinuation(Object candidate) {
		return isContinuationType(candidate.getClass());
	}

	public static boolean isContinuationType(Class<?> candidate) {
		return KOTLIN_CONTINUATION_CLASS != null && KOTLIN_CONTINUATION_CLASS.isAssignableFrom(candidate);
	}

	@SuppressWarnings("unchecked")
	public static <T> T monoAwaitSingleOrNull(Mono<? extends T> source, Object continuation) {
		Assert.notNull(KOTLIN_CONTINUATION_CLASS, "Kotlin Coroutines library is not present in classpath");
		Assert.isAssignable(KOTLIN_CONTINUATION_CLASS, continuation.getClass());
		return (T) kotlinx.coroutines.reactor.MonoKt.awaitSingleOrNull(
				source, (kotlin.coroutines.Continuation<T>) continuation);
	}

	private CoroutinesUtils() {
	}

}
