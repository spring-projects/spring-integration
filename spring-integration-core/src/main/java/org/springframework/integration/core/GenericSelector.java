/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.core;

/**
 * Generic (lambda) strategy interface for selector.
 *
 * @param <S> the source type to accept.
 *
 * @author Artem Bilan
 * @since 4.0
 */
@FunctionalInterface
public interface GenericSelector<S> {

	boolean accept(S source);

}
