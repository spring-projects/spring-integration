/*
 * Copyright © 2020 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2020-present the original author or authors.
 */

package org.springframework.integration.kafka.dsl;

import org.springframework.integration.kafka.channel.SubscribableKafkaChannel;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Spec for a point to point channel backed by an Apache Kafka topic.
 *
 * @author Gary Russell
 *
 * @since 5.4
 *
 */
public class KafkaPointToPointChannelSpec extends KafkaSubscribableChannelSpec<SubscribableKafkaChannel> {

	protected KafkaPointToPointChannelSpec(KafkaTemplate<?, ?> template, KafkaListenerContainerFactory<?> factory,
			String topic) {

		this.channel = new SubscribableKafkaChannel(template, factory, topic);
	}

}
