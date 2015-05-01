/*
 * Copyright 2002-2013 the original author or authors.
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

import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerConfig;

import org.springframework.beans.factory.FactoryBean;

/**
 * @author Soby Chacko
 * @author Marius Bogoevici
 *
 * @since 0.5
 */
public class ProducerFactoryBean<K, V> implements FactoryBean<Producer<K, V>> {

	private static final Log LOGGER = LogFactory.getLog(ProducerFactoryBean.class);

	private final String brokerList;

	private final ProducerMetadata<K, V> producerMetadata;

	private Properties producerProperties = new Properties();

	public ProducerFactoryBean(final ProducerMetadata<K, V> producerMetadata, final String brokerList,
														 final Properties producerProperties) {
		this.producerMetadata = producerMetadata;
		this.brokerList = brokerList;
		if (producerProperties != null) {
			this.producerProperties = producerProperties;
		}
	}

	public ProducerFactoryBean(final ProducerMetadata<K, V> producerMetadata, final String brokerList) {
		this(producerMetadata, brokerList, null);
	}

	@Override
	public Producer<K, V> getObject() throws Exception {
		final Properties props = new Properties();
		props.putAll(producerProperties);
		props.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, brokerList);
		props.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, producerMetadata.getCompressionType().name());
		LOGGER.info("Using producer properties => " + props);
		return new KafkaProducer<>(props,
				producerMetadata.getKeySerializer(),
				producerMetadata.getValueSerializer());
	}

	@Override
	public Class<?> getObjectType() {
		return Producer.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}
}
