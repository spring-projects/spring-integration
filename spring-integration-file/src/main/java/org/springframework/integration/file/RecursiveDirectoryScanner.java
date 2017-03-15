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

package org.springframework.integration.file;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.integration.file.filters.AbstractFileListFilter;
import org.springframework.integration.file.filters.FileListFilter;

/**
 * The {@link DefaultDirectoryScanner} extension which walks through the directory tree
 * using {@link Files#walk}.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 *
 * @see Files#walk
 */
public class RecursiveDirectoryScanner extends DefaultDirectoryScanner {

	@Override
	public List<File> listFiles(File directory) throws IllegalArgumentException {
		FileListFilter<File> filter = getFilter();
		boolean supportAcceptFilter = filter instanceof AbstractFileListFilter;
		try {
			Stream<File> fileStream = Files.walk(directory.toPath())
					.skip(1)
					.map(Path::toFile)
					.filter(file -> !supportAcceptFilter
							|| ((AbstractFileListFilter<File>) filter).accept(file));

			if (supportAcceptFilter) {
				return fileStream.collect(Collectors.toList());
			}
			else {
				return filter.filterFiles(fileStream.toArray(File[]::new));
			}
		}
		catch (IOException e) {
			throw new IllegalArgumentException(e);
		}
	}

}
