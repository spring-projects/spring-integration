/*
 * Copyright © 2017 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2017-present the original author or authors.
 */

package org.springframework.integration.file.filters;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.springframework.util.Assert;

/**
 * The {@link CompositeFileListFilter} extension which chains the result
 * of the previous filter to the next one. If a filter in the chain returns
 * an empty list, the remaining filters are not invoked.
 *
 * @param <F> The type that will be filtered.
 *
 * @author Artem Bilan
 * @author Gary Russell
 * @author Cengis Kocaurlu
 *
 * @since 4.3.7
 *
 */
public class ChainFileListFilter<F> extends CompositeFileListFilter<F> {

	public ChainFileListFilter() {
	}

	public ChainFileListFilter(Collection<? extends FileListFilter<F>> fileFilters) {
		super(fileFilters);
	}

	@Override
	public List<F> filterFiles(F[] files) {
		Assert.notNull(files, "'files' should not be null");
		List<F> leftOver = Arrays.asList(files);
		for (FileListFilter<F> fileFilter : this.fileFilters) {
			if (leftOver.isEmpty()) {
				break;
			}
			@SuppressWarnings("unchecked")
			F[] fileArray = leftOver.toArray((F[]) Array.newInstance(leftOver.get(0).getClass(), leftOver.size()));
			leftOver = fileFilter.filterFiles(fileArray);
		}
		return leftOver;
	}

	@Override
	public boolean accept(F file) {
		// we can't use stream().allMatch() because there is no guarantee of early exit
		for (FileListFilter<F> filter : this.fileFilters) {
			if (!filter.accept(file)) {
				return false;
			}
		}
		return true;
	}

}
