/*
 * Copyright 2025-present the original author or authors.
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
