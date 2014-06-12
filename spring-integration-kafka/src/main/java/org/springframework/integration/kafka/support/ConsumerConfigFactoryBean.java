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

import kafka.consumer.ConsumerConfig;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.FactoryBean;

/**
 * @author Soby Chacko
 * @since 0.5
 */
public class ConsumerConfigFactoryBean<K,V> implements FactoryBean<ConsumerConfig> {

	private static final Log LOGGER = LogFactory.getLog(ConsumerConfigFactoryBean.class);
	private final ConsumerMetadata<K,V> consumerMetadata;
	private final ZookeeperConnect zookeeperConnect;
    private Properties consumerProperties = new Properties();

	public ConsumerConfigFactoryBean(final ConsumerMetadata<K,V> consumerMetadata,
			final ZookeeperConnect zookeeperConnect, final Properties consumerProperties) {
		this.consumerMetadata = consumerMetadata;
		this.zookeeperConnect = zookeeperConnect;
		if (consumerProperties != null) {
			this.consumerProperties = consumerProperties;
		}
	}

    public ConsumerConfigFactoryBean(final ConsumerMetadata<K, V> consumerMetadata,
			final ZookeeperConnect zookeeperConnect) {
        this(consumerMetadata, zookeeperConnect, null);
    }

	@Override
	public ConsumerConfig getObject() throws Exception {
		final Properties properties = new Properties();
		properties.putAll(consumerProperties);
		properties.put("zookeeper.connect", zookeeperConnect.getZkConnect());
		properties.put("zookeeper.session.timeout.ms", zookeeperConnect.getZkSessionTimeout());
		properties.put("zookeeper.sync.time.ms", zookeeperConnect.getZkSyncTime());

		// Overriding the default value of -1, which will make the consumer to
		// wait indefinitely
		if (!properties.containsKey("consumer.timeout.ms")) {
			properties.put("consumer.timeout.ms", consumerMetadata.getConsumerTimeout());
		}

		properties.put("group.id", consumerMetadata.getGroupId());

        LOGGER.info("Using consumer properties => " + properties);

		return new ConsumerConfig(properties);
	}

	@Override
	public Class<?> getObjectType() {
		return ConsumerConfig.class;
	}

	@Override
	public boolean isSingleton() {
		return true;
	}
}
