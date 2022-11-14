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

import reactor.core.publisher.Mono;

import org.springframework.core.KotlinDetector;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

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

	public static boolean isContinuation(Object candidate) {
		return isContinuationType(candidate.getClass());
	}

	public static boolean isContinuationType(Class<?> candidate) {
		return KotlinDetector.isKotlinPresent() && kotlin.coroutines.Continuation.class.isAssignableFrom(candidate);
	}

	@Nullable
	@SuppressWarnings("unchecked")
	public static <T> T monoAwaitSingleOrNull(Mono<? extends T> source, Object continuation) {
		Assert.state(isContinuation(continuation), () ->
				"The 'continuation' must be an instance of 'kotlin.coroutines.Continuation', but it is: "
						+ continuation.getClass());
		return (T) kotlinx.coroutines.reactor.MonoKt.awaitSingleOrNull(
				source, (kotlin.coroutines.Continuation<T>) continuation);
	}

	private CoroutinesUtils() {
	}

}
