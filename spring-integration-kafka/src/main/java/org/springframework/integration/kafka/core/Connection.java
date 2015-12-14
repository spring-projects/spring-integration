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

import java.util.Map;

import kafka.api.OffsetRequest;

/**
 * A connection to a Kafka broker.
 *
 * @author Marius Bogoevici
 */
public interface Connection {

	/**
	 * Fetch data from a Kafka broker.
	 * @param fetchRequests a list of fetch operations
	 * @return message batches, indexed by partition
	 * @throws ConsumerException the ConsumerException if any underlying error
	 */
	Result<KafkaMessageBatch> fetch(FetchRequest... fetchRequests) throws ConsumerException;

	/**
	 * Fetch an actual offset in the partition, immediately before the given reference time,
	 * or the smallest and largest value, respectively, if the special values -1
	 * ({@link OffsetRequest#LatestTime()}) and -2 ({@link OffsetRequest#EarliestTime()})
	 * are used . To be used to position the initial offset of a read operation.
	 * @param referenceTime The returned values will be before this time, if they exist. The special
	 * values -2 ({@link OffsetRequest#EarliestTime()}) and -1 ({@link OffsetRequest#LatestTime()}) are supported.
	 * @param partitions the offsets, indexed by {@link Partition}
	 * @return any errors, an empty {@link Result} in case of success
	 * @throws ConsumerException the ConsumerException if any underlying error
	 */
	Result<Long> fetchInitialOffset(long referenceTime, Partition... partitions) throws ConsumerException;

	/**
	 * Fetch offsets from the native Kafka offset management system.
	 * @param consumerId the id of the consumer
	 * @param partitions the list of partitions whose offsets are queried for
	 * @return any errors, an empty {@link Result} in case of success
	 * @throws ConsumerException the ConsumerException if any underlying error
	 */
	Result<Long> fetchStoredOffsetsForConsumer(String consumerId, Partition... partitions) throws ConsumerException;

	/**
	 * Update offsets in the native Kafka offset management system.
	 * @param consumerId the id of the consumer
	 * @param offsets the offsets, indexed by {@link Partition}
	 * @return any errors, an empty {@link Result} in case of success
	 * @throws ConsumerException the ConsumerException if any underlying error
	 */
	Result<Void> commitOffsetsForConsumer(String consumerId, Map<Partition, Long> offsets) throws ConsumerException;

	/**
	 * Retrieve the leader broker addresses for all the partitions in the given topics.
	 * @param topics the topics whose partitions we query for
	 * @return broker addresses, indexed by {@link Partition}
	 * @throws ConsumerException the ConsumerException if any underlying error
	 * @deprecated as of 1.3, only {@link ConnectionFactory#getLeaders(Iterable)} should be used
	 */
	@Deprecated
	Result<BrokerAddress> findLeaders(String... topics) throws ConsumerException;


	/**
	 * The broker address for this consumer
	 * @return broker address
	 */
	BrokerAddress getBrokerAddress();

	/**
	 * Closes the connection to the broker. No further operations are permitted.
	 */
	void close();

}
