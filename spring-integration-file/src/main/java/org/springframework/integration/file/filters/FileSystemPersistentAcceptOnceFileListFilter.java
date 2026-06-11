/*
 * Copyright 2013-present the original author or authors.
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
import java.io.IOException;
import java.io.UncheckedIOException;

import org.springframework.integration.metadata.ConcurrentMetadataStore;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 3.0
 *
 */
public class FileSystemPersistentAcceptOnceFileListFilter extends AbstractPersistentAcceptOnceFileListFilter<File> {

	public FileSystemPersistentAcceptOnceFileListFilter(ConcurrentMetadataStore store, String prefix) {
		super(store, prefix);
	}

	@Override
	protected long modified(File file) {
		return file.lastModified();
	}

	@Override
	protected String fileName(File file) {
		try {
			return file.getCanonicalPath();
		}
		catch (IOException ex) {
			throw new UncheckedIOException(ex);
		}
	}

	/**
	 * Check that the file still exists to avoid a race condition when multithreaded and
	 * another thread removed the file while we were waiting for the lock.
	 * @since 4.3.19
	 */
	@Override
	protected boolean fileStillExists(File file) {
		return file.exists();
	}

	@Override
	protected boolean isDirectory(File file) {
		return file.isDirectory();
	}

}
