/*
 * Copyright 2017 the original author or authors.
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

import java.util.Arrays;
import java.util.List;

import org.springframework.util.Assert;

/**
 * The {@link CompositeFileListFilter} extension which chains the result
 * of the previous filter to the next one.
 *
 * @author Artem Bilan
 *
 * @since 4.3.7
 *
 * @param <F> The type that will be filtered.
 */
public class ChainFileListFilter<F> extends CompositeFileListFilter<F> {

	@Override
	public List<F> filterFiles(F[] files) {
		Assert.notNull(files, "'files' should not be null");
		List<F> leftOver = Arrays.asList(files);
		for (FileListFilter<F> fileFilter : this.fileFilters) {
			@SuppressWarnings("unchecked")
			F[] fileArray = (F[]) leftOver.toArray();
			leftOver = fileFilter.filterFiles(fileArray);
		}
		return leftOver;
	}

}
