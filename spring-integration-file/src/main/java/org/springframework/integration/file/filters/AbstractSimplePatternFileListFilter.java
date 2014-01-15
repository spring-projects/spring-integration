/*
 * Copyright 2002-2014 the original author or authors.
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

import org.springframework.util.AntPathMatcher;

/**
 * Base class for filters that support ant style path expressions, which are less powerful
 * but more readable than regular expressions. This filter only filters on the name of the
 * file, the rest of the path is ignored.
 *
 * @author Iwein Fuld
 * @author Mark Fisher
 * @see org.springframework.util.AntPathMatcher
 * @see org.springframework.integration.file.filters.AbstractRegexPatternFileListFilter
 * @since 2.0
 */
public abstract class AbstractSimplePatternFileListFilter<F> extends AbstractFileListFilter<F> {

	private final AntPathMatcher matcher = new AntPathMatcher();

	private final String path;


	public AbstractSimplePatternFileListFilter(String path) {
		this.path = path;
	}


	/**
	 * Accepts the given file if its name matches the pattern.
	 */
	@Override
	public final boolean accept(F file) {
		return this.matcher.match(this.path, this.getFilename(file));
	}

	/**
	 * Subclasses must implement this method to extract the file's name.
	 *
	 * @param file The file.
	 * @return The file name.
	 */
	protected abstract String getFilename(F file);

}
