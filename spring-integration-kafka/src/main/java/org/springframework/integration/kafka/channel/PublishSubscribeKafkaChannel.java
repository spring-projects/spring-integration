/*
 * Copyright 2020-present the original author or authors.
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
