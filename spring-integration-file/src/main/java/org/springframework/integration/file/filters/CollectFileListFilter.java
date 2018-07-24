/*
 * Copyright 2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.file.filters;

import java.util.ArrayList;
import java.util.List;

import org.springframework.util.Assert;

/**
 * The {@link CompositeFileListFilter} extension which collects the result of all filters.
 *
 * @param <F> The type that will be filtered.
 * @author Alen Turkovic
 * @since 5.1
 */
public class CollectFileListFilter<F> extends CompositeFileListFilter<F> {

	@Override
	public List<F> filterFiles(F[] files) {
		Assert.notNull(files, "'files' should not be null");
		List<F> results = new ArrayList<>();
		for (FileListFilter<F> fileFilter : this.fileFilters) {
			results.addAll(fileFilter.filterFiles(files));
		}
		return results;
	}
}
