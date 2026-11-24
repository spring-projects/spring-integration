/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.kafka.dsl;

import org.springframework.integration.kafka.channel.PublishSubscribeKafkaChannel;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Spec for a publish/subscribe channel backed by an Apache Kafka topic.
 *
 * @author Gary Russell
 *
 * @since 5.4
 *
 */
public class KafkaPublishSubscribeChannelSpec
		extends KafkaSubscribableChannelSpec<PublishSubscribeKafkaChannel> {

	protected KafkaPublishSubscribeChannelSpec(KafkaTemplate<?, ?> template, KafkaListenerContainerFactory<?> factory,
			String topic) {

		this.channel = new PublishSubscribeKafkaChannel(template, factory, topic);
	}

}
