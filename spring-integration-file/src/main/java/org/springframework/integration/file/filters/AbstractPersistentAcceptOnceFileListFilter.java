/*
 * Copyright 2013-2021 the original author or authors.
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

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.util.List;

import org.springframework.integration.metadata.ConcurrentMetadataStore;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * Stores "seen" files in a MetadataStore to survive application restarts.
 * The default key is 'prefix' plus the absolute file name; value is the timestamp of the file.
 * Files are deemed as already 'seen' if they exist in the store and have the
 * same modified time as the current file.
 *
 * @param <F> the file type.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 3.0
 *
 */
public abstract class AbstractPersistentAcceptOnceFileListFilter<F> extends AbstractDirectoryAwareFileListFilter<F>
		implements ReversibleFileListFilter<F>, ResettableFileListFilter<F>, Closeable {

	protected final ConcurrentMetadataStore store; // NOSONAR

	protected final String prefix; // NOSONAR

	@Nullable
	protected final Flushable flushableStore; // NOSONAR

	protected boolean flushOnUpdate; // NOSONAR

	public AbstractPersistentAcceptOnceFileListFilter(ConcurrentMetadataStore store, String prefix) {
		Assert.notNull(store, "'store' cannot be null");
		Assert.notNull(prefix, "'prefix' cannot be null");
		this.store = store;
		this.prefix = prefix;
		if (store instanceof Flushable) {
			this.flushableStore = (Flushable) store;
		}
		else {
			this.flushableStore = null;
		}
	}

	/**
	 * Determine whether the metadataStore should be flushed on each update (if {@link Flushable}).
	 * @param flushOnUpdate true to flush.
	 * @since 4.1.5
	 */
	public void setFlushOnUpdate(boolean flushOnUpdate) {
		this.flushOnUpdate = flushOnUpdate;
	}

	@Override
	public boolean accept(F file) {
		if (alwaysAccept(file)) {
			return true;
		}
		String key = buildKey(file);
		String newValue = value(file);
		String oldValue = this.store.putIfAbsent(key, newValue);
		if (oldValue == null) { // not in store
			flushIfNeeded();
			return fileStillExists(file);
		}
		// same value in store
		if (!isEqual(file, oldValue) && this.store.replace(key, oldValue, newValue)) {
			flushIfNeeded();
			return fileStillExists(file);
		}
		return false;
	}

	/**
	 * Check if the file still exists; default implementation returns true.
	 * @param file the file.
	 * @return true if the filter should return true.
	 * @since 4.3.19
	 */
	protected boolean fileStillExists(F file) {
		return true;
	}

	/**
	 * {@inheritDoc}
	 * @since 4.0.4
	 */
	@Override
	public void rollback(F file, List<F> files) {
		// If file must be removed all subsequent files should be removed as well
		boolean rollingBack = false;
		for (F fileToRollback : files) {
			if (fileToRollback.equals(file)) {
				rollingBack = true;
			}
			if (rollingBack) {
				remove(fileToRollback);
			}
		}
	}

	@Override
	public boolean remove(F fileToRemove) {
		String removed = this.store.remove(buildKey(fileToRemove));
		flushIfNeeded();
		return removed != null;
	}

	@Override
	public void close() throws IOException {
		if (this.store instanceof Closeable) {
			((Closeable) this.store).close();
		}
	}

	/**
	 * The default value stored for the key is the last modified date.
	 * @param file The file.
	 * @return The value to store for the file.
	 */
	private String value(F file) {
		return Long.toString(modified(file));
	}

	/**
	 * Override this method if you wish to use something other than the
	 * modified timestamp to determine equality.
	 * @param file The file.
	 * @param value The current value for the key in the store.
	 * @return true if equal.
	 */
	protected boolean isEqual(F file, String value) {
		return Long.parseLong(value) == modified(file);
	}

	/**
	 * The default key is the {@link #prefix} plus the full filename.
	 * @param file The file.
	 * @return The key.
	 */
	protected String buildKey(F file) {
		return this.prefix + fileName(file);
	}

	/**
	 * Flush the store if it's a {@link Flushable} and
	 * {@link #setFlushOnUpdate(boolean) flushOnUpdate} is true.
	 * @since 1.4.5
	 */
	protected void flushIfNeeded() {
		if (this.flushOnUpdate && this.flushableStore != null) {
			try {
				this.flushableStore.flush();
			}
			catch (@SuppressWarnings("unused") IOException e) {
				// store's responsibility to log
			}
		}
	}

	@Override
	protected boolean isDirectory(F file) {
		return false;
	}

	protected abstract long modified(F file);

	protected abstract String fileName(F file);

}
