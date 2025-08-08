/*
 * Copyright © 2023 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2023-present the original author or authors.
 */

package org.springframework.integration.util;

import java.util.function.Function;

/**
 * A Function-like interface which allows throwing Error.
 *
 * @param <T> the input type.
 * @param <R> the output type.
 * @param <E> the throwable type.
 *
 * @author Artem Bilan
 *
 * @since 6.1
 */
@FunctionalInterface
public interface CheckedFunction<T, R, E extends Throwable> {

	R apply(T t) throws E;

	default Function<T, R> unchecked() {
		return t1 -> {
			try {
				return apply(t1);
			}
			catch (Throwable t) { // NOSONAR
				if (t instanceof RuntimeException runtimeException) { // NOSONAR
					throw runtimeException;
				}
				else if (t instanceof Error error) { // NOSONAR
					throw error;
				}
				else {
					throw new IllegalStateException(t);
				}
			}
		};
	}

}
