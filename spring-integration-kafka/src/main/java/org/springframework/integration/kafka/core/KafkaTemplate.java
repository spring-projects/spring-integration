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

import com.gs.collections.api.block.function.Function;
import com.gs.collections.api.list.MutableList;
import com.gs.collections.impl.list.mutable.FastList;


/**
 * A template for executing high-level operations on a set of Kafka brokers.
 *
 * @author Marius Bogoevici
 */
public class KafkaTemplate implements KafkaOperations {

	private final ConnectionFactory connectionFactory;

	private FetchRequestToLeaderBrokerAddress fetchRequestToLeaderBrokerAddress = new FetchRequestToLeaderBrokerAddress();

	public KafkaTemplate(ConnectionFactory connectionFactory) {
		this.connectionFactory = connectionFactory;
	}

	public ConnectionFactory getConnectionFactory() {
		return connectionFactory;
	}

	@Override
	public Result<KafkaMessageBatch> receive(Iterable<FetchRequest> messageFetchRequests) {
		FastList<FetchRequest> requestList = FastList.newList(messageFetchRequests);
		MutableList<BrokerAddress> distinctBrokerAddresses =
				requestList.collect(fetchRequestToLeaderBrokerAddress).distinct();
		if (distinctBrokerAddresses.size() != 1) {
			throw new IllegalArgumentException("All messages must be fetched from the same broker");
		}
		return connectionFactory.connect(distinctBrokerAddresses.getFirst())
				.fetch(requestList.toTypedArray(FetchRequest.class));
	}

	@SuppressWarnings("serial")
	private class FetchRequestToLeaderBrokerAddress implements Function<FetchRequest, BrokerAddress> {

		@Override
		public BrokerAddress valueOf(FetchRequest fetchRequest) {
			return connectionFactory.getLeader(fetchRequest.getPartition());
		}

	}

}
