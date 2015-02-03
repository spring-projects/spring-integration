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

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.integration.kafka.core.BrokerAddress;
import org.springframework.integration.kafka.core.Connection;
import org.springframework.integration.kafka.core.ConnectionFactory;
import org.springframework.integration.kafka.core.ConsumerException;
import org.springframework.integration.kafka.core.KafkaConsumerDefaults;
import org.springframework.integration.kafka.core.Partition;
import org.springframework.integration.kafka.core.PartitionNotFoundException;
import org.springframework.integration.kafka.core.Result;
import org.springframework.util.Assert;

import kafka.common.ErrorMapping;

/**
 * Base implementation for {@link OffsetManager}. Subclasses may customize functionality as necessary.
 *
 * @author Marius Bogoevici
 */
public abstract class AbstractOffsetManager implements OffsetManager, DisposableBean {

	protected final Log log = LogFactory.getLog(this.getClass());

	protected String consumerId = KafkaConsumerDefaults.GROUP_ID;

	protected long referenceTimestamp = KafkaConsumerDefaults.DEFAULT_OFFSET_RESET;

	protected ConnectionFactory connectionFactory;

	protected Map<Partition, Long> initialOffsets;

	public AbstractOffsetManager(ConnectionFactory connectionFactory) {
		this(connectionFactory, new HashMap<Partition, Long>());
	}

	public AbstractOffsetManager(ConnectionFactory connectionFactory, Map<Partition, Long> initialOffsets) {
		Assert.notNull(connectionFactory, "A 'connectionFactory' can't be null");
		Assert.notNull(initialOffsets, "An initialOffsets can't be null");
		this.connectionFactory = connectionFactory;
		this.initialOffsets = initialOffsets;
	}

	public String getConsumerId() {
		return this.consumerId;
	}

	/**
	 * The identifier of a consumer of Kafka messages. Allows to manage offsets separately by consumer
	 *
	 * @param consumerId the consumer ID
	 */
	public void setConsumerId(String consumerId) {
		this.consumerId = consumerId;
	}

	/**
	 * A timestamp to be used for resetting initial offsets
	 *
	 * @param referenceTimestamp the reset timestamp for initial offsets
	 */
	public void setReferenceTimestamp(long referenceTimestamp) {
		this.referenceTimestamp = referenceTimestamp;
	}

	@Override
	public void destroy() throws Exception {
		try {
			this.flush();
		}
		catch (IOException e) {
			log.error("Error while flushing the OffsetManager", e);
		}
		try {
			this.close();
		}
		catch (IOException e) {
			log.error("Error while closing the OffsetManager", e);
		}
	}

	/**
	 * @see OffsetManager#updateOffset(Partition, long)
	 */
	@Override
	public synchronized final void updateOffset(Partition partition, long offset) {
		doUpdateOffset(partition, offset);
	}

	/**
	 * @see OffsetManager#getOffset(Partition)
	 */
	@Override
	public synchronized final long getOffset(Partition partition) {
		Long storedOffset = doGetOffset(partition);
		if (storedOffset == null) {
			if (this.initialOffsets.containsKey(partition)) {
				return this.initialOffsets.get(partition);
			}
			else {
				BrokerAddress leader = this.connectionFactory.getLeader(partition);
				if (leader == null) {
					throw new PartitionNotFoundException(partition);
				}
				Connection connection = this.connectionFactory.connect(leader);
				Result<Long> offsetResult = connection.fetchInitialOffset(this.referenceTimestamp, partition);
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
			return storedOffset;
		}
	}

	@Override
	public synchronized void resetOffsets(Collection<Partition> partitionsToReset) {
		// any information about those offsets is invalid and can be ignored
		for (Partition partition : partitionsToReset) {
			doRemoveOffset(partition);
			this.initialOffsets.remove(partition);
		}
	}

	@Override
	public synchronized void deleteOffset(Partition partition) {
		doRemoveOffset(partition);
	}

	protected abstract void doUpdateOffset(Partition partition, long offset);

	protected abstract void doRemoveOffset(Partition partition);

	protected abstract Long doGetOffset(Partition partition);

}
