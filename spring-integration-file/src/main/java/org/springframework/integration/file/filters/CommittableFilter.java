/*
 * Copyright 2002-2016 the original author or authors.
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

/**
 *
 * A {@link FileListFilter} that allows the caller to commit state
 * changes.
 *
 * @author Bojan Vukasovic
 * @since 5.0
 *
 */
public interface CommittableFilter<F> extends FileListFilter<F> {
	/**
	 * In order to flag file as PROCESSED, we have to know
	 * when transaction finished. On transaction commit, we call this.
	 * @param file The files.
	 */
	void commit(F file);
}
