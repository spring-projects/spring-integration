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
 * Used to configure a {@link DefaultConnectionFactory}. Provides a list of seed brokers.
 *
 * @author Marius Bogoevici
 */
public interface Configuration {

	/**
	 * The minimum amount of data that a server fetch operation will wait for before returning,
	 * unless {@code maxWait} has elapsed.
	 * In conjunction with {@link Configuration#getMaxWait()}}, controls latency
	 * and throughput.
	 * Smaller values increase responsiveness, but may increase the number of poll operations,
	 * potentially reducing throughput and increasing CPU consumption.
	 * @return the minimum amount of data for a fetch operation
	 */
	int getMinBytes();

	/**
	 * The maximum amount of time that a server fetch operation will wait before returning
	 * (unless {@code minFetchSizeInBytes}) are available.
	 * In conjunction with {@link AbstractConfiguration#setMinBytes(int)},
	 * controls latency and throughput.
	 * Smaller intervals increase responsiveness, but may increase
	 * the number of poll operations, potentially increasing CPU
	 * consumption and reducing throughput.
	 * @return the maximum wait time for a fetch operation
	 */
	int getMaxWait();

	/**
	 * The client name to be used throughout this connection.
	 * @return the client id for a connection
	 */
	String getClientId();

	/**
	 * The buffer size for this client
	 * @return the buffer size
	 */
	int getBufferSize();

	/**
	 * The socket timeout for this client
	 * @return the socket timeout
	 */
	int getSocketTimeout();

	/**
	 * The retry backoff for this client
	 * @return the retry backoff
	 * @since 1.3
	 */
	int getBackOff();

	/**
	 * The timeout on fetching metadata (e.g. partition leaders)
	 * @return the fetch metadata timeout
	 */
	int getFetchMetadataTimeout();

	/**
	 * The list of seed broker addresses used by this Configuration.
	 * @return the broker addresses
	 */
	List<BrokerAddress> getBrokerAddresses();

	/**
	 * A list of default partitions to perform operations on.
	 * @return the list of partitions
	 */
	List<Partition> getDefaultPartitions();

	/**
	 * A default topic to perform operations on.
	 * @return a topic name
	 */
	String getDefaultTopic();


}
