/*
 * Copyright 2016-2021 the original author or authors.
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

/**
 * A file list filter that can be configured to always accept (pass) directories.
 * This permits, for example, pattern matching on just files when using recursion
 * to examine a directory tree.
 *
 * @param <F> the file type.
 *
 * @author Gary Russell
 *
 * @since 5.0
 *
 */
public abstract class AbstractDirectoryAwareFileListFilter<F> extends AbstractFileListFilter<F> {

	private boolean alwaysAcceptDirectories;

	private boolean forRecursion;

	/**
	 * Set to true so that filters that support this feature can unconditionally pass
	 * directories; default false.
	 * @param alwaysAcceptDirectories true to always pass directories.
	 */
	public void setAlwaysAcceptDirectories(boolean alwaysAcceptDirectories) {
		this.alwaysAcceptDirectories = alwaysAcceptDirectories;
	}

	@Override
	public boolean isForRecursion() {
		return this.forRecursion;
	}

	/**
	 * Set to true to inform a recursive gateway operation to use the full file path as
	 * the metadata key. Also sets {@link #alwaysAcceptDirectories}.
	 * @param forRecursion true to use the full path.
	 * @since 5.3.6
	 */
	public void setForRecursion(boolean forRecursion) {
		this.forRecursion = forRecursion;
		this.alwaysAcceptDirectories = forRecursion;
	}

	protected boolean alwaysAccept(F file) {
		return file != null && this.alwaysAcceptDirectories && isDirectory(file);
	}

	/**
	 * Subclasses must implement this method to indicate whether the file
	 * is a directory or not.
	 * @param file the file.
	 * @return true if it's a directory.
	 */
	protected abstract boolean isDirectory(F file);

}
