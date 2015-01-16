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

import java.util.List;

/**
 * A group of {@link KafkaMessage}s retrieved by a fetch operation
 *
 * @author Marius Bogoevici
 */
public class KafkaMessageBatch {

	private Partition partition;

	private List<KafkaMessage> messages;

	private long highWatermark;

	public KafkaMessageBatch(Partition partition, List<KafkaMessage> messages, long highWatermark) {
		this.partition = partition;
		this.messages = messages;
		this.highWatermark = highWatermark;
	}

	public Partition getPartition() {
		return partition;
	}

	public void setPartition(Partition partition) {
		this.partition = partition;
	}

	public List<KafkaMessage> getMessages() {
		return messages;
	}

	public void setMessages(List<KafkaMessage> messages) {
		this.messages = messages;
	}

	public long getHighWatermark() {
		return highWatermark;
	}

	public void setHighWatermark(long highWatermark) {
		this.highWatermark = highWatermark;
	}

}
