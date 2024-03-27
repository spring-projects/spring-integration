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

/**
 * Filter that supports ant style path expressions, which are less powerful but more readable than regular expressions.
 * This filter only filters on the name of the file, the rest of the path is ignored.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @since 2.0
 */
public class SimplePatternFileListFilter extends AbstractSimplePatternFileListFilter<File> {

	public SimplePatternFileListFilter(String path) {
		super(path);
	}

	@Override
	protected String getFilename(File file) {
		return file.getName();
	}

	@Override
	protected boolean isDirectory(File file) {
		return file.isDirectory();
	}

}
