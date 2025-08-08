/*
 * Copyright © 2022 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2022-present the original author or authors.
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
