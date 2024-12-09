/*
 * Copyright 2023-2024 the original author or authors.
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
	 * Will be restored back, but with a proper {@link Callable<T>} return type.
	 */
	@Deprecated
	default Runnable unchecked() {
		return this::uncheckedCallable;
	}

	/**
	 * Wrap the {@link #call()} into unchecked {@link Callable<T>}.
	 * Re-throw its exception wrapped with a {@link IllegalStateException}.
	 * Will be replaced with a proper {@link #unchecked()} implementation in 6.5.
	 * @return the unchecked {@link Callable<T>}.
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
