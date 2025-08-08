/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.util;

import java.util.Collection;

/**
 * Base strategy for filtering out a subset of a Collection of elements.
 *
 * @param <T> the collection element type.
 *
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 *
 * @since 2.1
 */
@FunctionalInterface
public interface CollectionFilter<T> {

	Collection<T> filter(Collection<T> unfilteredElements);

}
