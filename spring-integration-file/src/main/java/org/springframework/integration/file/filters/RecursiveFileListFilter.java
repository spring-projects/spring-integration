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

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Simple {@link FileListFilter} implementation which runs recursively
 * and passes all found files to the delegate to filter.
 *
 * @author Alen Turkovic
 */
public class RecursiveFileListFilter implements FileListFilter<File> {

	private final FileListFilter<File> delegate;

	public RecursiveFileListFilter(final FileListFilter<File> delegate) {
		this.delegate = delegate;
	}

	@Override
	public List<File> filterFiles(final File[] files) {
		return Stream.of(files)
				.flatMap(file -> {
					if (file.isDirectory()) {
						return filterFiles(file.listFiles()).stream();
					}
					return this.delegate.filterFiles(new File[]{file}).stream();
				})
				.collect(Collectors.toList());
	}
}
