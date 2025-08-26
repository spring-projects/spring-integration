/*
 * Copyright 2025-present the original author or authors.
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

import org.jspecify.annotations.Nullable;

import org.springframework.core.AttributeAccessor;

/**
 * Error handler-like strategy to provide fallback based on the {@link AttributeAccessor}.
 * @param <T> the type that is returned from the recovery
 *
 * @author Artem Bilan
 *
 * @since 7.0
 */
public interface RecoveryCallback<T extends @Nullable Object> {

	/**
	 * @param context the context for failure
	 * @param cause the cause of the failure
	 * @return an Object that can be used to replace the callback result that failed
	 */
	T recover(AttributeAccessor context, Throwable cause);

}
