/*
 * Copyright 2022-present the original author or authors.
 */

package org.springframework.integration.util;

import kotlin.coroutines.Continuation;
import kotlinx.coroutines.reactor.MonoKt;
import org.jspecify.annotations.Nullable;
import reactor.core.publisher.Mono;

import org.springframework.core.KotlinDetector;
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
		return KotlinDetector.isKotlinPresent() && Continuation.class.isAssignableFrom(candidate);
	}

	@Nullable
	@SuppressWarnings("unchecked")
	public static <T> T monoAwaitSingleOrNull(Mono<? extends T> source, Object continuation) {
		Assert.state(isContinuation(continuation), () ->
				"The 'continuation' must be an instance of 'kotlin.coroutines.Continuation', but it is: "
						+ continuation.getClass());
		return (T) MonoKt.awaitSingleOrNull(
				source, (Continuation<T>) continuation);
	}

	private CoroutinesUtils() {
	}

}
