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
