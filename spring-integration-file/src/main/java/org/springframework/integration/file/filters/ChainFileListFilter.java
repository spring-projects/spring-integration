/*
 * Copyright 2017-2019 the original author or authors.
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
