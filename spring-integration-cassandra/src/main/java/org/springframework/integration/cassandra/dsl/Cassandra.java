/*
 * Copyright 2022 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.cassandra.dsl;

import org.springframework.data.cassandra.core.ReactiveCassandraOperations;
import org.springframework.integration.cassandra.outbound.CassandraMessageHandler;

/**
 * Factory class for Apache Cassandra components DSL.
 *
 * @author Artem Bilan
 *
 * @since 6.0
 */
public final class Cassandra {

	/**
	 * Create an instance of {@link CassandraMessageHandlerSpec} for the provided {@link ReactiveCassandraOperations}.
	 * @param cassandraOperations the {@link ReactiveCassandraOperations} to use.
	 * @return the spec.
	 */
	public static CassandraMessageHandlerSpec outboundChannelAdapter(ReactiveCassandraOperations cassandraOperations) {
		return new CassandraMessageHandlerSpec(cassandraOperations);
	}

	/**
	 * Create an instance of {@link CassandraMessageHandlerSpec} for the provided {@link ReactiveCassandraOperations}.
	 * @param cassandraOperations the {@link ReactiveCassandraOperations} to use.
	 * @param queryType the {@link CassandraMessageHandler.Type} to use.
	 * @return the spec.
	 */
	public static CassandraMessageHandlerSpec outboundChannelAdapter(ReactiveCassandraOperations cassandraOperations,
			CassandraMessageHandler.Type queryType) {

		return new CassandraMessageHandlerSpec(cassandraOperations, queryType);
	}

	/**
	 * Create an instance of {@link CassandraMessageHandlerSpec} for the provided {@link ReactiveCassandraOperations}
	 * in an outbound gateway mode.
	 * @param cassandraOperations the {@link ReactiveCassandraOperations} to use.
	 * @return the spec.
	 */
	public static CassandraMessageHandlerSpec outboundGateway(ReactiveCassandraOperations cassandraOperations) {
		return new CassandraMessageHandlerSpec(cassandraOperations)
				.producesReply(true);
	}

	/**
	 * Create an instance of {@link CassandraMessageHandlerSpec} for the provided {@link ReactiveCassandraOperations}
	 * in an outbound gateway mode.
	 * @param cassandraOperations the {@link ReactiveCassandraOperations} to use.
	 * @param queryType the {@link CassandraMessageHandler.Type} to use.
	 * @return the spec.
	 */
	public static CassandraMessageHandlerSpec outboundGateway(ReactiveCassandraOperations cassandraOperations,
			CassandraMessageHandler.Type queryType) {

		return new CassandraMessageHandlerSpec(cassandraOperations, queryType)
				.producesReply(true);
	}

	private Cassandra() {
	}

}
