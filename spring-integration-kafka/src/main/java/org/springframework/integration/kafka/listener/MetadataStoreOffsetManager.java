/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.kafka.listener;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;
import java.util.Map;

import org.springframework.integration.kafka.core.ConnectionFactory;
import org.springframework.integration.kafka.core.Partition;
import org.springframework.integration.metadata.MetadataStore;
import org.springframework.integration.metadata.SimpleMetadataStore;

/**
 * An {@link OffsetManager} that persists offsets into a {@link MetadataStore}.
 *
 * @author Marius Bogoevici
 */
public class MetadataStoreOffsetManager extends AbstractOffsetManager {

	private MetadataStore metadataStore = new SimpleMetadataStore();

	public MetadataStoreOffsetManager(ConnectionFactory connectionFactory) {
		super(connectionFactory);
	}

	public MetadataStoreOffsetManager(ConnectionFactory connectionFactory, Map<Partition, Long> initialOffsets) {
		super(connectionFactory, initialOffsets);
	}

	/**
	 * The backing {@link MetadataStore} for storing offsets.
	 *
	 * @param metadataStore a fully configured {@link MetadataStore} instance
	 */
	public void setMetadataStore(MetadataStore metadataStore) {
		this.metadataStore = metadataStore;
	}

	@Override
	public void close() throws IOException {
		// Flush before closing. This may be redundant, but ensures that the metadata store is closed properly
		flush();
		if (this.metadataStore instanceof Closeable) {
			((Closeable) this.metadataStore).close();
		}
	}

	@Override
	public void flush() throws IOException {
		if (this.metadataStore instanceof Flushable) {
			((Flushable) this.metadataStore).flush();
		}
	}

	@Override
	protected void doUpdateOffset(Partition partition, long offset) {
		this.metadataStore.put(generateKey(partition), Long.toString(offset));
	}

	@Override
	protected void doRemoveOffset(Partition partition) {
		this.metadataStore.remove(generateKey(partition));
	}

	protected Long doGetOffset(Partition partition) {
		String storedOffsetValueAsString = this.metadataStore.get(generateKey(partition));
		Long storedOffsetValue = null;
		if (storedOffsetValueAsString != null) {
			try {
				storedOffsetValue = Long.parseLong(storedOffsetValueAsString);
			}
			catch (NumberFormatException e) {
				log.warn("Invalid value: " + storedOffsetValueAsString);
			}
		}
		return storedOffsetValue;
	}

	public String generateKey(Partition partition) {
		return partition.getTopic() + ":" + partition.getId() + ":" + getConsumerId();
	}

}
