/*
 * Copyright © 2023 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2023-present the original author or authors.
 */

package org.springframework.integration.util;

import java.util.concurrent.Callable;

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
public interface CheckedCallable<T, E extends Throwable> {

	T call() throws E;

	/**
	 * Wrap the {@link #call()} into unchecked {@link Runnable} (by mistake).
	 * Re-throw its exception wrapped with a {@link IllegalStateException}.
	 * @return the Runnable (by mistake).
	 * @deprecated since 6.3.7 in favor of {@link #uncheckedCallable()}.
	 * Will be restored back, but with a proper {@link Callable} return type.
	 */
	@Deprecated
	default Runnable unchecked() {
		return this::uncheckedCallable;
	}

	/**
	 * Wrap the {@link #call()} into unchecked {@link Callable}.
	 * Re-throw its exception wrapped with a {@link IllegalStateException}.
	 * Will be replaced with a proper {@link #unchecked()} implementation in 6.5.
	 * @return the unchecked {@link Callable}.
	 * @since 6.3.7
	 */
	default Callable<T> uncheckedCallable() {
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
