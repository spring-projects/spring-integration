/*
 * Copyright 2015 the original author or authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.util;

/**
 * Implementations of this class perform work on the given parameter
 * and return a result of an optionally different type.
 *
 * <p>This is a copy of Java 8 {@code Function} interface.
 *
 * @param <T> The type of the input to the apply operation
 * @param <R> The type of the result of the apply operation
 *
 * @author Jon Brisbin
 * @author Stephane Maldini
 * @since 4.0
 */
public interface Function<T, R> {

	/**
	 * Execute the logic of the action, accepting the given parameter.
	 * @param t The parameter to pass to the action.
	 * @return result
	 */
	R apply(T t);

}

