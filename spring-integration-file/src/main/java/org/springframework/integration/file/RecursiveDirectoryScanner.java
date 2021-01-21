/*
 * Copyright 2017-2021 the original author or authors.
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
import java.io.IOException;
import java.nio.file.FileVisitOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.springframework.integration.file.filters.AbstractFileListFilter;
import org.springframework.integration.file.filters.FileListFilter;
import org.springframework.util.Assert;

/**
 * The {@link DefaultDirectoryScanner} extension which walks through the directory tree
 * using {@link Files#walk(Path, int, FileVisitOption...)}.
 * <p>
 * By default this class visits all levels of the file tree without any {@link FileVisitOption}s.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 5.0
 *
 * @see Files#walk
 */
public class RecursiveDirectoryScanner extends DefaultDirectoryScanner {

	private int maxDepth = Integer.MAX_VALUE;

	private FileVisitOption[] fileVisitOptions = new FileVisitOption[0];

	/**
	 * The maximum number of directory levels to visit.
	 * @param maxDepth the maximum number of directory levels to visit
	 */
	public void setMaxDepth(int maxDepth) {
		this.maxDepth = maxDepth;
	}

	/**
	 * The options to configure the traversal.
	 * @param fileVisitOptions options to configure the traversal
	 */
	public void setFileVisitOptions(FileVisitOption... fileVisitOptions) {
		Assert.notNull(fileVisitOptions, "'fileVisitOptions' must not be null");
		this.fileVisitOptions = Arrays.copyOf(fileVisitOptions, fileVisitOptions.length);
	}

	@Override
	public List<File> listFiles(File directory) throws IllegalArgumentException {
		FileListFilter<File> filter = getFilter();
		boolean supportAcceptFilter = filter instanceof AbstractFileListFilter;
		try (Stream<Path> pathStream = Files.walk(directory.toPath(), this.maxDepth, this.fileVisitOptions);) {
			Stream<File> fileStream =
					pathStream
							.skip(1) // NOSONAR
							.map(Path::toFile)
							.filter(file -> !supportAcceptFilter || filter.accept(file));

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
