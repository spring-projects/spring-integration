/*
 * Copyright 2014-2022 the original author or authors.
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

package org.springframework.integration.core;

/**
 * Generic (lambda) strategy interface for transformer.
 *
 * @param <S> the source type - 'transform from'.
 * @param <T> the target type - 'transform to'.
 *
 * @author Artem Bilan
 * @since 4.0
 */
@FunctionalInterface
public interface GenericTransformer<S, T> {

	T transform(S source);

}
