/*
 * Copyright © 2018 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2018-present the original author or authors.
 */

package org.springframework.integration.acks;

/**
 * A factory for creating {@link AcknowledgmentCallback}s.
 *
 * @param <T> a type containing information with which to populate the acknowledgment.
 *
 * @author Gary Russell
 *
 * @since 5.0.1
 *
 */
@FunctionalInterface
public interface AcknowledgmentCallbackFactory<T> {

	/**
	 * Create the callback.
	 * @param info information for the callback to process the acknowledgment.
	 * @return the callback
	 */
	AcknowledgmentCallback createCallback(T info);

}
