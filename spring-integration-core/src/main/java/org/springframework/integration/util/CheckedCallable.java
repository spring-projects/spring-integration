/*
 * Copyright 2023-2025 the original author or authors.
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

	/**
	 * Wrap the {@link #call()} into unchecked {@link Callable}.
	 * Re-throw its exception wrapped with a {@link IllegalStateException}.
	 * @return the unchecked {@link Callable}.
	 * @since 6.3.7
	 * @deprecated since 6.5 in favor of {@link #unchecked()}.
	 */
	@Deprecated(since = "6.5", forRemoval = true)
	default Callable<T> uncheckedCallable() {
		return unchecked();
	}

}
