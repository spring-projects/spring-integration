/*
 * Copyright 2014 the original author or authors.
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

import java.util.List;

/**
 *
 * A {@link FileListFilter} that allows the caller to reverse (roll back) state
 * changes.
 *
 * @author Gary Russell
 * @since 4.0.4
 *
 */
public interface ReversibleFileListFilter<F> extends FileListFilter<F> {

	/**
	 * Indicate that not all files previously passed by this filter (in {@link #filterFiles(Object[])}
	 * have been processed; the file must be in the list of files; it, and all files after it, will
	 * be considered to have not been processed and will be considered next time.
	 * @param file the file which failed.
	 * @param files the list of files that were returned by {@link #filterFiles(Object[])}.
	 */
	void rollback(F file, List<F> files);

}
