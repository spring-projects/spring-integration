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

import java.io.File;
import java.util.Map;
import java.util.function.Function;

/**
 * File system implementation of {@link AbstractMarkerFilePresentFileListFilter}.
 *
 * @author Gary Russell
 * @since 5.0
 *
 */
public class FileSystemMarkerFilePresentFileListFilter extends AbstractMarkerFilePresentFileListFilter<File> {

	FileSystemMarkerFilePresentFileListFilter(FileNameFileListFilter<File> filter) {
		super(filter);
	}

	FileSystemMarkerFilePresentFileListFilter(FileNameFileListFilter<File> filter, String suffix) {
		super(filter, suffix);
	}

	FileSystemMarkerFilePresentFileListFilter(FileNameFileListFilter<File> filter,
			Function<String, String> function) {
		super(filter, function);
	}

	FileSystemMarkerFilePresentFileListFilter(
			Map<FileNameFileListFilter<File>, Function<String, String>> filtersAndFunctions) {
		super(filtersAndFunctions);
	}

	@Override
	protected String getFilename(File file) {
		return file.getName();
	}

}
