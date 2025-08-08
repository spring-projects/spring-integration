/*
 * Copyright © 2020 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2020-present the original author or authors.
 */

package org.springframework.integration.kafka.dsl;

import org.springframework.integration.dsl.MessageChannelSpec;
import org.springframework.integration.kafka.channel.AbstractKafkaChannel;
import org.springframework.lang.Nullable;

/**
 *
 * Spec for a message channel backed by an Apache Kafka topic.
 *
 * @param <S> the spec type.
 * @param <C> the channel type.
 *
 * @author Gary Russell
 *
 * @since 5.4
 *
 */
public abstract class AbstractKafkaChannelSpec<S extends AbstractKafkaChannelSpec<S, C>, C extends AbstractKafkaChannel>
		extends MessageChannelSpec<S, C> {

	@Nullable
	protected String groupId; // NOSONAR

	@Override
	public S id(@Nullable String idToSet) { // NOSONAR - increase visibility
		return super.id(idToSet);
	}

	/**
	 * Set the group id to use on the consumer side.
	 * @param group the group id.
	 * @return the spec.
	 */
	public S groupId(String group) {
		this.groupId = group;
		return _this();
	}

}
