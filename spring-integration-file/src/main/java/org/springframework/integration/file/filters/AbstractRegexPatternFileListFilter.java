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

import java.util.regex.Pattern;

import org.springframework.util.Assert;

/**
 * Filters a listing of files by qualifying their 'name'
 * against a regular expression (an instance of {@link Pattern}).
 *
 * @param <F> the type of file entry
 *
 * @author Iwein Fuld
 * @author Josh Long
 * @author Artem Bilan
 *
 * @since 2.0
 */
public abstract class AbstractRegexPatternFileListFilter<F> extends AbstractDirectoryAwareFileListFilter<F> {

	private Pattern pattern;

	public AbstractRegexPatternFileListFilter(String pattern) {
		this(Pattern.compile(pattern));
	}

	public AbstractRegexPatternFileListFilter(Pattern pattern) {
		Assert.notNull(pattern, "'pattern' must not be null!");
		this.pattern = pattern;
	}

	public void setPattern(String pattern) {
		Assert.notNull(pattern, "'pattern' must not be null!");
		setPattern(Pattern.compile(pattern));
	}

	public void setPattern(Pattern pattern) {
		Assert.notNull(pattern, "'pattern' must not be null!");
		this.pattern = pattern;
	}

	@Override
	public boolean accept(F file) {
		return alwaysAccept(file) || (file != null && this.pattern.matcher(getFilename(file)).matches());
	}

	/**
	 * Subclasses must implement this method to extract the file's name.
	 * @param file The file.
	 * @return The file name.
	 */
	protected abstract String getFilename(F file);

}
