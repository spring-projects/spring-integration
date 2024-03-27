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

package org.springframework.integration.file.filters;

import java.io.File;
import java.util.regex.Pattern;

/**
 * Implementation of AbstractRegexPatternMatchingFileListFilter for java.io.File instances.
 *
 * @author Mark Fisher
 * @author Gary Russell
 */
public class RegexPatternFileListFilter extends AbstractRegexPatternFileListFilter<File> {

	public RegexPatternFileListFilter(String pattern) {
		super(pattern);
	}

	public RegexPatternFileListFilter(Pattern pattern) {
		super(pattern);
	}

	@Override
	protected String getFilename(File file) {
		return (file != null) ? file.getName() : null;
	}

	@Override
	protected boolean isDirectory(File file) {
		return file.isDirectory();
	}

}
