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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.DisposableBean;
import org.springframework.integration.kafka.core.KafkaConsumerDefaults;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.util.CollectionUtils;

/**
 * @author Soby Chacko
 * @author Ilayaperumal Gopinathan
 * @since 0.5
 */
public class KafkaConsumerContext<K, V> implements DisposableBean {
	private Map<String, ConsumerConfiguration<K, V>> consumerConfigurations;

	private String consumerTimeout = KafkaConsumerDefaults.CONSUMER_TIMEOUT;

	private ZookeeperConnect zookeeperConnect;

	public String getConsumerTimeout() {
		return this.consumerTimeout;
	}

	public void setConsumerTimeout(final String consumerTimeout) {
		this.consumerTimeout = consumerTimeout;
	}

	public ZookeeperConnect getZookeeperConnect() {
		return this.zookeeperConnect;
	}

	public void setZookeeperConnect(final ZookeeperConnect zookeeperConnect) {
		this.zookeeperConnect = zookeeperConnect;
	}

	public void setConsumerConfigurations(Map<String, ConsumerConfiguration<K, V>> consumerConfigurations) {
		this.consumerConfigurations = consumerConfigurations;
	}

	public Map<String, ConsumerConfiguration<K, V>> getConsumerConfigurations() {
		return this.consumerConfigurations;
	}

	public ConsumerConfiguration<K, V> getConsumerConfiguration(String groupId) {
		return this.consumerConfigurations.get(groupId);
	}

	public Message<Map<String, Map<Integer, List<Object>>>> receive() {
		final Map<String, Map<Integer, List<Object>>> consumedData = new HashMap<String, Map<Integer, List<Object>>>();

		for (final ConsumerConfiguration<K, V> consumerConfiguration : getConsumerConfigurations().values()) {
			final Map<String, Map<Integer, List<Object>>> messages = consumerConfiguration.receive();

			if (!CollectionUtils.isEmpty(messages)) {
				consumedData.putAll(messages);
			}
		}
		return consumedData.isEmpty() ? null : MessageBuilder.withPayload(consumedData).build();
	}

	@Override
	public void destroy() throws Exception {
		for (ConsumerConfiguration<K, V> config : this.consumerConfigurations.values()) {
			config.shutdown();
		}
	}

}
