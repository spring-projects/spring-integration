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
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import kafka.common.ErrorMapping;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.kafka.core.BrokerAddress;
import org.springframework.integration.kafka.core.Connection;
import org.springframework.integration.kafka.core.ConnectionFactory;
import org.springframework.integration.kafka.core.ConsumerException;
import org.springframework.integration.kafka.core.KafkaConsumerDefaults;
import org.springframework.integration.kafka.core.Partition;
import org.springframework.integration.kafka.core.Result;
import org.springframework.integration.metadata.MetadataStore;
import org.springframework.integration.metadata.SimpleMetadataStore;

/**
 * An {@link OffsetManager} that persists offsets into a {@link MetadataStore}.
 *
 * @author Marius Bogoevici
 */
public class MetadataStoreOffsetManager implements OffsetManager {

	private final static Log log = LogFactory.getLog(MetadataStoreOffsetManager.class);

	private String consumerId = KafkaConsumerDefaults.GROUP_ID;

	private MetadataStore metadataStore = new SimpleMetadataStore();

	private ConnectionFactory connectionFactory;

	private Map<Partition, Long> initialOffsets;

	private long referenceTimestamp = KafkaConsumerDefaults.DEFAULT_OFFSET_RESET;

	public MetadataStoreOffsetManager(ConnectionFactory connectionFactory) {
		this(connectionFactory, null);
	}

	public MetadataStoreOffsetManager(ConnectionFactory connectionFactory, Map<Partition, Long> initialOffsets) {
		this.connectionFactory = connectionFactory;
		this.initialOffsets = initialOffsets == null? new HashMap<Partition, Long>() : initialOffsets;
	}

	public String getConsumerId() {
		return consumerId;
	}

	/**
	 * The identifier of a consumer of Kafka messages. Allows to store separate sets of offsets in the
	 * {@link MetadataStore}.
	 * @param consumerId the consumer ID
	 */
	public void setConsumerId(String consumerId) {
		this.consumerId = consumerId;
	}

	public MetadataStore getMetadataStore() {
		return metadataStore;
	}

	/**
	 * The backing {@link MetadataStore} for storing offsets.
	 * @param metadataStore a fully configured {@link MetadataStore} instance
	 */
	public void setMetadataStore(MetadataStore metadataStore) {
		this.metadataStore = metadataStore;
	}

	public long getReferenceTimestamp() {
		return referenceTimestamp;
	}

	/**
	 * A timestamp to be used for resetting initial offsets, if they are not available in the {@link MetadataStore}
	 * @param referenceTimestamp the reset timestamp for initial offsets
	 */
	public void setReferenceTimestamp(long referenceTimestamp) {
		this.referenceTimestamp = referenceTimestamp;
	}

	/**
	 * @see OffsetManager#updateOffset(Partition, long)
	 */
	@Override
	public synchronized void updateOffset(Partition partition, long offset) {
		metadataStore.put(asKey(partition), Long.toString(offset));
	}

	/**
	 * @see OffsetManager#getOffset(Partition)
	 */
	@Override
	public synchronized long getOffset(Partition partition) {
		Long offsetInMetadataStore = getOffsetFromMetadataStore(partition);
		if (offsetInMetadataStore == null) {
			if (this.initialOffsets.containsKey(partition)) {
				return this.initialOffsets.get(partition);
			}
			else {
				BrokerAddress leader = this.connectionFactory.getLeader(partition);
				if (leader == null) {
					throw new ConsumerException("No leader found for " + partition.toString());
				}
				Connection connection = this.connectionFactory.connect(leader);
				Result<Long> offsetResult = connection.fetchInitialOffset(referenceTimestamp, partition);
				if (offsetResult.getErrors().size() > 0) {
					throw new ConsumerException(ErrorMapping.exceptionFor(offsetResult.getError(partition)));
				}
				if (!offsetResult.getResults().containsKey(partition)) {
					throw new IllegalStateException("Result does not contain an expected value");
				}
				return offsetResult.getResult(partition);
			}
		}
		else {
			return offsetInMetadataStore;
		}
	}

	@Override
	public synchronized void resetOffsets(Collection<Partition> partitionsToReset) {
		// any information about those offsets is invalid and can be ignored
		for (Partition partition : partitionsToReset) {
			metadataStore.remove(asKey(partition));
			initialOffsets.remove(partition);
		}
	}

	@Override
	public void close() throws IOException {
		// Flush before closing. This may be redundant, but ensures that the metadata store is closed properly
		flush();
		if (metadataStore instanceof Closeable) {
			((Closeable) metadataStore).close();
		}
	}

	@Override
	public void flush() throws IOException {
		if (metadataStore instanceof Flushable) {
			((Flushable) metadataStore).flush();
		}
	}


	private Long getOffsetFromMetadataStore(Partition partition) {
		String storedOffsetValueAsString = this.metadataStore.get(asKey(partition));
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

	private String asKey(Partition partition) {
		return partition.getTopic() + ":" + partition.getId() + ":" + consumerId;
	}

}
