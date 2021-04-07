/*
 * Copyright 2020 the original author or authors.
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
