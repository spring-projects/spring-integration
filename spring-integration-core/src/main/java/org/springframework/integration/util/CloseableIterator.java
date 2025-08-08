/*
 * Copyright © 2018 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2018-present the original author or authors.
 */

package org.springframework.integration.util;

import java.util.Iterator;

/**
 * A {@link CloseableIterator} is intended to be used when it may hold resources (such as file or socket handles).
 * This allows implementations to clean up any resources they need to keep open to iterate over elements.
 *
 * @param <E> the iterator element type.
 *
 * @author Ruslan Stelmachenko
 * @author Gary Russell
 *
 * @since 4.3.15
 */
public interface CloseableIterator<E> extends Iterator<E>, AutoCloseable {

	@Override
	void close(); // override throws Exception

}
