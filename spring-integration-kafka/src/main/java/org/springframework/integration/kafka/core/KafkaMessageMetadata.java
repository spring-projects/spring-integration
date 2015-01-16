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

package org.springframework.integration.kafka.core;

import kafka.message.Message;

import org.springframework.util.Assert;

/**
 * Metadata for a Kafka {@link Message}.
 *
 * @author Marius Bogoevici
 */
public class KafkaMessageMetadata {

	private final long offset;

	private final long nextOffset;

	private final Partition partition;

	public KafkaMessageMetadata(Partition partition, long offset, long nextOffset) {
		Assert.notNull(partition);
		Assert.isTrue(offset >= 0);
		Assert.isTrue(nextOffset >= 0);
		this.offset = offset;
		this.nextOffset = nextOffset;
		this.partition = partition;
	}

	public Partition getPartition() {
		return partition;
	}

	public long getOffset() {
		return offset;
	}

	public long getNextOffset() {
		return nextOffset;
	}

	@Override
	public String toString() {
		return "KafkaMessageMetadata [" +
				"offset=" + offset +
				", nextOffset=" + nextOffset +
				", " + partition.toString();
	}

}
