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

import java.util.Collection;
import java.util.Map;

/**
 * Creates Kafka connections and retrieves metadata for topics and partitions.
 *
 * @author Marius Bogoevici
 *
 */
public interface ConnectionFactory {

	/**
	 * Create a connection to a Kafka broker, caching it internally
	 * @param brokerAddress a broker address
	 * @return a working connection
	 */
	Connection connect(BrokerAddress brokerAddress);

	/**
	 * Retrieve the leaders for a set of partitions.
	 * @param partitions whose leaders are queried
	 * @return the broker associated with the provided topic and partition
	 */
	Map<Partition, BrokerAddress> getLeaders(Iterable<Partition> partitions);

	/**
	 * Return the leader for a single partition
	 * @param partition the partition whose leader is queried
	 * @return the leader's address
	 */
	BrokerAddress getLeader(Partition partition);

	/**
	 * Refresh the cached metadata (i.e. leader topology and partitions). To be called when the topology changes
	 * are detected (i.e. brokers leave and/or partition leaders change) and that results in fetch errors,
	 * for instance.
	 * @param topics the topics for which to refresh the leaders
	 */
	void refreshMetadata(Collection<String> topics);

	/**
	 * Retrieves the partitions of a given topic
	 * @param topic the topic to query for
	 * @return a list of partitions
	 */
	Collection<Partition> getPartitions(String topic);

}
