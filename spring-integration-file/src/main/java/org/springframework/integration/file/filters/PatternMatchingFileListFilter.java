/*
 * Copyright 2002-2008 the original author or authors.
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

import org.springframework.integration.file.entries.FileEntryNameExtractor;
import org.springframework.integration.file.entries.PatternMatchingEntryListFilter;
import org.springframework.util.Assert;

import java.io.File;
import java.util.List;
import java.util.regex.Pattern;

/**
 * An {@link org.springframework.integration.file.entries.EntryListFilter} implementation that matches a File against a {@link Pattern}.
 *
 * @author Iwein Fuld
 * @author Mark Fisher
 *
 * @since 1.0.0
 */
public class PatternMatchingFileListFilter extends PatternMatchingEntryListFilter<File> implements FileListFilter{

	/**
	 * Create a file filter for the given pattern.
	 */
	public PatternMatchingFileListFilter(Pattern pattern) {
		super(new FileEntryNameExtractor(), pattern);
	}

	public PatternMatchingFileListFilter(String pattern) {
		super(new FileEntryNameExtractor(), pattern);
	}

	/**
	 * Filter out the files of which the name doesn't match the pattern of this filter
	 */
	public List<File> filterFiles(File[] files) {
		Assert.notNull(files, "'files' must not be null.");
		return this.filterEntries(files);
	}
}
