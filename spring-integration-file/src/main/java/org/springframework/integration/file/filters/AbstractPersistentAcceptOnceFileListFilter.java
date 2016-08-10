/*
 * Copyright 2013-2016 the original author or authors.
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

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.util.List;

import org.springframework.integration.metadata.ConcurrentMetadataStore;
import org.springframework.util.Assert;

/**
 * Stores "seen" files in a MetadataStore to survive application restarts.
 * The default key is 'prefix' plus the absolute file name; value is the timestamp of the file.
 * Files are deemed as already 'seen' if they exist in the store and have the
 * same modified time as the current file.
 *
 * @author Gary Russell
 * @since 3.0
 *
 */
public abstract class AbstractPersistentAcceptOnceFileListFilter<F> extends AbstractFileListFilter<F>
		implements ReversibleFileListFilter<F>, ResettableFileListFilter<F>,  Closeable {

	protected final ConcurrentMetadataStore store;

	protected final Flushable flushableStore;

	protected final String prefix;

	protected volatile boolean flushOnUpdate;

	private final Object monitor = new Object();

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
	protected boolean accept(F file) {
		String key = buildKey(file);
		synchronized (this.monitor) {
			String newValue = value(file);
			String oldValue = this.store.putIfAbsent(key, newValue);
			if (oldValue == null) { // not in store
				flushIfNeeded();
				return true;
			}
			// same value in store
			if (!isEqual(file, oldValue) && this.store.replace(key, oldValue, newValue)) {
				flushIfNeeded();
				return true;
			}
			return false;
		}
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
		return Long.toString(this.modified(file));
	}

	/**
	 * Override this method if you wish to use something other than the
	 * modified timestamp to determine equality.
	 * @param file The file.
	 * @param value The current value for the key in the store.
	 * @return true if equal.
	 */
	protected boolean isEqual(F file, String value) {
		return Long.valueOf(value) == this.modified(file);
	}

	/**
	 * The default key is the {@link #prefix} plus the full filename.
	 * @param file The file.
	 * @return The key.
	 */
	protected String buildKey(F file) {
		return this.prefix + this.fileName(file);
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
			catch (IOException e) {
				// store's responsibility to log
			}
		}
	}

	protected abstract long modified(F file);

	protected abstract String fileName(F file);

}
