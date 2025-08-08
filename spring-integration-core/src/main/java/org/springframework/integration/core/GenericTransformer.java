/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
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
