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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.integration.file.support.FileProcessingRecord;
import org.springframework.integration.file.support.FileProcessingRecordSerializer;
import org.springframework.integration.file.support.FileProcessingStatus;
import org.springframework.integration.metadata.ConcurrentMetadataStore;
import org.springframework.util.Assert;

/**
 * Stores "seen" files in a MetadataStore to survive application restarts.
 * The default key is 'prefix' plus the absolute file name; value is the timestamp of the file.
 * Files are deemed as already 'seen' if they exist in the store and have the
 * flag set to PROCESSED. In case that files are REJECTED in store files are already 'seen'.
 * If file is marked as PROCESSING, file is marked as 'seen' up to processingTTL time. After this
 * time, file is selected for re-processing.
 *
 * @author Bojan Vukasovic
 * @since 5.0
 *
 */
public abstract class AbstractPersistentCommittableAcceptOnceFileListFilter<F>
		implements ResettableFileListFilter<F>, CommittableFilter<F>, Closeable {

	protected final ConcurrentMetadataStore store;

	protected final Flushable flushableStore;

	protected final String prefix;

	protected volatile boolean flushOnUpdate;

	private final Object monitor = new Object();

	protected int maxRetries = -1;	//default - unlimited

	protected long processingTTL = 30000;	//default - 30 seconds

	protected int acceptedFilesPerRun = -1;	//default - unlimited

	public AbstractPersistentCommittableAcceptOnceFileListFilter(ConcurrentMetadataStore store, String prefix) {
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
	 */
	public void setFlushOnUpdate(boolean flushOnUpdate) {
		this.flushOnUpdate = flushOnUpdate;
	}

	/**
	 * Set maximum number of retries for file that has been rejected to be re-processed again.
	 * Default value is -1 which means unlimited number of retries.
	 * @param maxRetries number of retries.
	 */
	public void setMaxRetries(int maxRetries) {
		this.maxRetries = maxRetries;
	}

	/**
	 * Set minimal number of time to wait before file that is flagged as PROCESSING is removed
	 * from list, and taken for re-processing. Useful in case when thread that processes file is
	 * killed in mid of processing.
	 * @param processingTTL time to wait for PROCESSING flag to be removed and file re-examined.
	 * @param timeUnit unit of processingTTL.
	 */
	public void setProcessingTTL(int processingTTL, TimeUnit timeUnit) {
		this.processingTTL = timeUnit.toMillis(processingTTL);
	}

	/**
	 * Limits number of files that are accepted per each run.
	 * @param acceptedFilesPerRun number of files that should be accepted per run.
	 */
	public void setAcceptedFilesPerRun(int acceptedFilesPerRun) {
		this.acceptedFilesPerRun = acceptedFilesPerRun;
	}

	@Override
	public void close() throws IOException {
		if (this.store instanceof Closeable) {
			((Closeable) this.store).close();
		}
	}

	@Override
	public void commit(F file) {
		String key = buildKey(file);
		synchronized (this.monitor) {
			while (true) {
				String oldValue = this.store.get(key);
				FileProcessingRecord record;

				if (oldValue != null) {
					record = FileProcessingRecordSerializer.deserialize(oldValue);
					if (record.getStatus() == FileProcessingStatus.PROCESSED) {
						//if another process finished processing this file and marked it as processed, that means that
						//we just processed it one more time - this can happen only for parallel workers on same files
						return;
					}
				}
				else {
					//should never happen - we finished processing file that is not marked for processing at all
					//meaning - somebody during the process manually deleted record from store
					record = new FileProcessingRecord(FileProcessingStatus.PROCESSED, modified(file), 0);
				}
				record.markAsProcessed();
				String newValue = FileProcessingRecordSerializer.serialize(record);
				boolean replaced = this.store.replace(key, oldValue, newValue);    //if not replaced - we have to try to add it
				if (!replaced) {
					oldValue = this.store.putIfAbsent(key, newValue);
					if (oldValue != null) {
						//means that somebody inserted it in table in the meantime - try replacing again
						continue;
					}
				}
				break;	//if we get here - it is replaced
			}
		}
	}

	@Override
	public boolean remove(F f) {
		String oldValue = this.store.remove(buildKey(f));
		flushIfNeeded();
		return oldValue != null;
	}

	@Override
	public List<F> filterFiles(F[] files) {
		List<F> accepted = new ArrayList<>();
		if (files != null) {
			int acceptedSoFar = 0;
			for (F file : files) {
				if (this.acceptedFilesPerRun > -1 && this.acceptedFilesPerRun > acceptedSoFar) {
					break;
				}
				if (this.accept(file)) {
					accepted.add(file);
					acceptedSoFar++;
				}
			}
		}
		return accepted;
	}

	public boolean accept(F file) {
		String key = buildKey(file);
		synchronized (this.monitor) {

			FileProcessingRecord record;
			while (true) {
				long currentTimestamp = System.currentTimeMillis();

				record = new FileProcessingRecord(FileProcessingStatus.PROCESSING, modified(file), currentTimestamp);

				String newValue = FileProcessingRecordSerializer.serialize(record);
				String oldValue = this.store.putIfAbsent(key, newValue);    //try to store if not already there

				if (oldValue != null) {
					//means that this file was already processed before (or possibly processed currently in parallel)
					oldValue = this.store.get(key);
					record = FileProcessingRecordSerializer.deserialize(oldValue);
					if (record.getStatus() == FileProcessingStatus.PROCESSED
							|| record.getStatus() == FileProcessingStatus.REJECTED) {
						return false;    //file has been already processed successfully or rejected
					}
					else if ((currentTimestamp - record.getLastRetry()) < this.processingTTL) {
						return false;    //file is probably still being processed (wait ttl to possibly retry)
					}
					if (this.maxRetries > 0 && record.getNumberOfRetries() >= this.maxRetries) {
						record.markAsRejected();    //if we exhausted retries, mark file as rejected
					}
					else {
						record.retriedAt(currentTimestamp);    //mark file as retried at this point in time
					}
					newValue = FileProcessingRecordSerializer.serialize(record);
					boolean replaced = this.store.replace(key, oldValue, newValue);	//if false - not replaced, so try to add again
					if (!replaced) {
						continue;	//means that somebody in between calls removed key from store
					}
				}
				break;
			}

			return record.getStatus() == FileProcessingStatus.PROCESSING;
		}
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
