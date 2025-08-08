/*
 * Copyright © 2020 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2020-present the original author or authors.
 */

package org.springframework.integration.kafka.channel;

import org.springframework.integration.channel.BroadcastCapableChannel;
import org.springframework.integration.dispatcher.BroadcastingDispatcher;
import org.springframework.integration.dispatcher.MessageDispatcher;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.KafkaOperations;

/**
 * Publish/subscribe channel backed by an Apache Kafka topic.
 *
 * @author Gary Russell
 *
 * @since 5.4
 *
 */
public class PublishSubscribeKafkaChannel extends SubscribableKafkaChannel implements BroadcastCapableChannel {

	/**
	 * Construct an instance with the provided parameters.
	 * @param template template for sending.
	 * @param factory factory for creating a container for receiving.
	 * @param channelTopic the topic.
	 */
	public PublishSubscribeKafkaChannel(KafkaOperations<?, ?> template, KafkaListenerContainerFactory<?> factory,
			String channelTopic) {

		super(template, factory, channelTopic);
	}

	@Override
	protected MessageDispatcher createDispatcher() {
		BroadcastingDispatcher broadcastingDispatcher = new BroadcastingDispatcher(true);
		broadcastingDispatcher.setBeanFactory(getBeanFactory());
		return broadcastingDispatcher;
	}

}
