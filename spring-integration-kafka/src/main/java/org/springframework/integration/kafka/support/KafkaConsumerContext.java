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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.integration.Message;
import org.springframework.integration.kafka.core.KafkaConsumerDefaults;
import org.springframework.integration.support.MessageBuilder;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Soby Chacko
 * @since 0.5
 */
public class KafkaConsumerContext implements BeanFactoryAware {
	private Map<String, ConsumerConfiguration> consumerConfigurations;
	private String consumerTimeout = KafkaConsumerDefaults.CONSUMER_TIMEOUT;
	private ZookeeperConnect zookeeperConnect;

	public Collection<ConsumerConfiguration> getConsumerConfigurations() {
		return consumerConfigurations.values();
	}

	@Override
	public void setBeanFactory(final BeanFactory beanFactory) throws BeansException {
		consumerConfigurations = ((ListableBeanFactory) beanFactory).getBeansOfType(ConsumerConfiguration.class);
	}

	public Message<Map<String, Map<Integer, List<Object>>>> receive() {
		final Map<String, Map<Integer, List<Object>>> consumedData = new HashMap<String, Map<Integer, List<Object>>>();

		for (final ConsumerConfiguration consumerConfiguration : getConsumerConfigurations()) {
			final Map<String, Map<Integer, List<Object>>> messages = consumerConfiguration.receive();

			if (messages != null){
				consumedData.putAll(messages);
			}
		}
		return MessageBuilder.withPayload(consumedData).build();
	}

	public String getConsumerTimeout() {
		return consumerTimeout;
	}

	public void setConsumerTimeout(final String consumerTimeout) {
		this.consumerTimeout = consumerTimeout;
	}

	public ZookeeperConnect getZookeeperConnect() {
		return zookeeperConnect;
	}

	public void setZookeeperConnect(final ZookeeperConnect zookeeperConnect) {
		this.zookeeperConnect = zookeeperConnect;
	}
}
