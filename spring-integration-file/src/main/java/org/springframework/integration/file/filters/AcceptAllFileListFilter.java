/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.file.filters;

/**
 * Simple implementation of {@link FileListFilter} that always returns true.
 * Suitable as a default.
 *
 * @param <F> The type that will be filtered.
 *
 * @author Iwein Fuld
 * @author Josh Long
 */
public class AcceptAllFileListFilter<F> extends AbstractFileListFilter<F> {

	@Override
	public boolean accept(F file) {
		return true;
	}

}
