/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.file;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.springframework.integration.file.filters.CompositeFileListFilter;
import org.springframework.integration.file.filters.FileListFilter;

/**
 * A custom scanner that only returns the first <code>maxNumberOfFiles</code>
 * elements from a directory listing. This is useful to limit the number of File
 * objects in memory and therefore mutually exclusive with {@code AcceptOnceFileListFilter}.
 * It should not be used in conjunction with an {@code AcceptOnceFileListFilter}.
 *
 * @author Iwein Fuld
 * @author Gary Russell
 * @since 2.0
 */
public class HeadDirectoryScanner extends DefaultDirectoryScanner {

	private final HeadFilter headFilter;

	public HeadDirectoryScanner(int maxNumberOfFiles) {
		this.headFilter = new HeadFilter(maxNumberOfFiles);
		setFilter(this.headFilter);
	}

	@Override
	public final void setFilter(FileListFilter<File> filter) {
		if (filter instanceof CompositeFileListFilter) {
			((CompositeFileListFilter<File>) filter).addFilter(this.headFilter);
			super.setFilter(filter);
		}
		else {
			CompositeFileListFilter<File> compositeFilter = new CompositeFileListFilter<File>();
			compositeFilter.addFilter(filter).addFilter(this.headFilter);
			super.setFilter(compositeFilter);
		}
	}

	private static final class HeadFilter implements FileListFilter<File> {

		private final int maxNumberOfFiles;

		private HeadFilter(int maxNumberOfFiles) {
			this.maxNumberOfFiles = maxNumberOfFiles;
		}

		@Override
		public List<File> filterFiles(File[] files) {
			return Arrays.asList(files).subList(0, Math.min(files.length, this.maxNumberOfFiles));
		}

	}

}
