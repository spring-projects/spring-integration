/*
 * Copyright 2023 the original author or authors.
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

import java.util.function.Function;

/**
 * A Function-like interface which allows throwing Error.
 *
 * @param <T> the input type.
 * @param <R> the output type.
 *
 * @author Artem Bilan
 *
 * @since 6.1
 */
@FunctionalInterface
public interface CheckedFunction<T, R> {

	R apply(T t) throws Throwable; // NOSONAR

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
