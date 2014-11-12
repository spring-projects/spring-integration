/*
 * Copyright 2002-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.kafka.support;

import java.util.Collection;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.messaging.Message;

/**
 * @author Soby Chacko
 * @author Rajasekar Elango
 * @author Ilayaperumal Gopinathan
 * @since 0.5
 */
public class KafkaProducerContext<K, V> {

	private static final Log LOGGER = LogFactory.getLog(KafkaProducerContext.class);

	private volatile Map<String, ProducerConfiguration<K, V>> producerConfigurations;

	private volatile ProducerConfiguration<K, V> theProducerConfiguration;

	private Properties producerProperties;

	public void send(final Message<?> message) throws Exception {
		if (message.getHeaders().containsKey("topic")) {
			ProducerConfiguration<K, V> producerConfiguration =
					getTopicConfiguration(message.getHeaders().get("topic", String.class));
			if (producerConfiguration != null) {
				producerConfiguration.send(message);
			}
		}
		// if there is a single producer configuration then use that config to send message.
		else if (this.theProducerConfiguration != null) {
			this.theProducerConfiguration.send(message);
		}
		else {
			throw new IllegalStateException("Could not send messages as there are multiple producer configurations " +
					"with no topic information found from the message header.");
		}
	}

	public ProducerConfiguration<K, V> getTopicConfiguration(final String topic) {
		if (this.theProducerConfiguration != null) {
			if (topic.matches(this.theProducerConfiguration.getProducerMetadata().getTopic())) {
				return this.theProducerConfiguration;
			}
		}

		Collection<ProducerConfiguration<K, V>> topics = this.producerConfigurations.values();

		for (final ProducerConfiguration<K, V> producerConfiguration : topics) {
			if (topic.matches(producerConfiguration.getProducerMetadata().getTopic())) {
				return producerConfiguration;
			}
		}
		LOGGER.error("No producer-configuration defined for topic " + topic + ". Cannot send message");
		return null;
	}

	public Map<String, ProducerConfiguration<K, V>> getProducerConfigurations() {
		return this.producerConfigurations;
	}

	public void setProducerConfigurations(Map<String, ProducerConfiguration<K, V>> producerConfigurations) {
		this.producerConfigurations = producerConfigurations;
		if (this.producerConfigurations.size() == 1) {
			this.theProducerConfiguration = this.producerConfigurations.values().iterator().next();
		}
	}

	/**
	 * @param producerProperties
	 *            The producerProperties to set.
	 */
	public void setProducerProperties(Properties producerProperties) {
		this.producerProperties = producerProperties;
	}

	/**
	 * @return Returns the producerProperties.
	 */
	public Properties getProducerProperties() {
		return this.producerProperties;
	}

}
