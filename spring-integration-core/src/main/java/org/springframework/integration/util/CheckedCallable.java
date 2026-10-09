/*
 * Copyright 2023-present the original author or authors.
 */

package org.springframework.integration.util;

import java.util.concurrent.Callable;

import org.jspecify.annotations.Nullable;

/**
 * A Callable-like interface which allows throwing any Throwable.
 * Checked exceptions are wrapped in an IllegalStateException.
 *
 * @param <T> the output type.
 * @param <E> the throwable type.
 *
 * @author Artem Bilan
 *
 * @since 6.2
 */
@FunctionalInterface
public interface CheckedCallable<T extends @Nullable Object, E extends Throwable> {

	T call() throws E;

	/**
	 * Wrap the {@link #call()} into unchecked {@link Callable}.
	 * Re-throw its exception wrapped with a {@link IllegalStateException}.
	 * @return the unchecked {@link Callable}.
	 */
	default Callable<T> unchecked() {
		return () -> {
			try {
				return call();
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
