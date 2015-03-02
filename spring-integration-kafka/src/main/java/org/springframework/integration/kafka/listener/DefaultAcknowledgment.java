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

import org.springframework.integration.kafka.core.KafkaMessage;
import org.springframework.integration.kafka.core.Partition;

/**
 * Default implementation for an {@link Acknowledgment} that defers to an underlying
 * {@link OffsetManager}.
 *
 * @author Marius Bogoevici
 * @since 1.0.1
 */
public class DefaultAcknowledgment implements Acknowledgment {

	private final OffsetManager offsetManager;

	private final Partition partition;

	private final Long offset;

	public DefaultAcknowledgment(OffsetManager offsetManager, Partition partition, Long offset) {
		this.offsetManager = offsetManager;
		this.partition = partition;
		this.offset = offset;
	}

	public DefaultAcknowledgment(OffsetManager offsetManager, KafkaMessage message) {
		this(offsetManager, message.getMetadata().getPartition(), message.getMetadata().getNextOffset());
	}

	@Override
	public void acknowledge() {
		offsetManager.updateOffset(partition, offset);
	}

}
