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
import java.util.Collection;

import org.springframework.integration.kafka.core.Partition;

/**
 * Stores and retrieves offsets for a Kafka consumer
 *
 * @author Marius Bogoevici
 */
public interface OffsetManager extends Closeable, Flushable {

	/**
	 * Updates the offset for a given {@link Partition}
	 * @param partition the partition whose offset is to be updated
	 * @param offset the new offset value
	 */
	void updateOffset(Partition partition, long offset);

	/**
	 * Retrieves the offset for a given {@link Partition}
	 *
	 * @param partition the partition to be
	 * @return the offset value
	 */
	long getOffset(Partition partition);

	/**
	 * Resets offsets for the given {@link Partition}s. To be invoked when the values stored are invalid,
	 * so a client cannot resume from that position. Implementations must decide on the best strategy to follow.
	 * @param partition to reset
	 */
	void resetOffsets(Collection<Partition> partition);

}
