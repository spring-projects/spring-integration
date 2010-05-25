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

package org.springframework.integration.file;

import java.io.File;
import java.util.regex.Pattern;

import org.springframework.util.Assert;

/**
 * A {@link FileListFilter} implementation that matches against a {@link Pattern}.
 * 
 * @author Mark Fisher
 */
public class PatternMatchingFileListFilter extends AbstractFileListFilter {

	private final Pattern pattern;


	/**
	 * Create a filter for the given pattern.
	 */
	public PatternMatchingFileListFilter(Pattern pattern) {
		Assert.notNull(pattern, "pattern must not be null");
		this.pattern = pattern;
	}


	protected boolean accept(File file) {
		return (file != null)
				&& this.pattern.matcher(file.getName()).matches();
	}

}
