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

import kafka.javaapi.producer.Producer;
import kafka.producer.ProducerConfig;
import kafka.producer.ProducerPool;
import kafka.producer.async.DefaultEventHandler;
import kafka.producer.async.EventHandler;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.FactoryBean;

import scala.collection.mutable.HashMap;

import java.util.Properties;

/**
 * @author Soby Chacko
 * @since 0.5
 */
public class ProducerFactoryBean<K,V> implements FactoryBean<Producer<K,V>> {

    private static final Log LOGGER = LogFactory.getLog(ProducerFactoryBean.class);

	private final String brokerList;
	private final ProducerMetadata<K,V> producerMetadata;
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
		props.put("metadata.broker.list", brokerList);
		props.put("compression.codec", producerMetadata.getCompressionCodec());

		if (producerMetadata.isAsync()){
			props.put("producer.type", "async");
			if (producerMetadata.getBatchNumMessages() != null){
				props.put("batch.num.messages", producerMetadata.getBatchNumMessages());
			}
		}

        LOGGER.info("Using producer properties => " + props);
		final ProducerConfig config = new ProducerConfig(props);
		final EventHandler<K, V> eventHandler = new DefaultEventHandler<K, V>(config,
				producerMetadata.getPartitioner() == null ? new DefaultPartitioner() : producerMetadata.getPartitioner(),
				producerMetadata.getValueEncoder(), producerMetadata.getKeyEncoder(),
				new ProducerPool(config), new HashMap<String, kafka.api.TopicMetadata>());

		final kafka.producer.Producer<K, V> prod = new kafka.producer.Producer<K, V>(config,
				eventHandler);
		return new Producer<K, V>(prod);
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
