/*
 * Copyright © 2022 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2022-present the original author or authors.
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
