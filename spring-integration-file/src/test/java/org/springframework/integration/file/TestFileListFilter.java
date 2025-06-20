/*
 * Copyright 2002-present the original author or authors.
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

import org.springframework.integration.file.filters.FileListFilter;

/**
 * @author Iwein Fuld
 * @author Mark Fisher
 */
public class TestFileListFilter implements FileListFilter<File> {

	public List<File> filterFiles(File[] entries) {
		return Arrays.asList(entries);
	}

}
