/*
 * Copyright 2015 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.springframework.integration.kafka.listener;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.junit.Rule;
import org.junit.Test;

import org.springframework.integration.kafka.core.BrokerAddress;
import org.springframework.integration.kafka.core.Connection;
import org.springframework.integration.kafka.core.DefaultConnectionFactory;
import org.springframework.integration.kafka.core.BrokerAddressListConfiguration;
import org.springframework.integration.kafka.core.Result;
import org.springframework.integration.kafka.core.Partition;
import org.springframework.integration.kafka.core.ZookeeperConfiguration;
import org.springframework.integration.kafka.rule.KafkaEmbedded;
import org.springframework.integration.kafka.support.ZookeeperConnect;

/**
 * @author Marius Bogoevici
 */
public class DefaultConnectionFactoryTests extends AbstractBrokerTests {

	@Rule
	public KafkaEmbedded kafkaEmbeddedBrokerRule = new KafkaEmbedded(1);

	@Override
	public KafkaEmbedded getKafkaRule() {
		return kafkaEmbeddedBrokerRule;
	}

	@Test
	public void testCreateConnectionFactoryWithBrokerList() throws Exception {

		createTopic(TEST_TOPIC, 1, 1, 1);

		BrokerAddress[] brokerAddresses = getKafkaRule().getBrokerAddresses();
		DefaultConnectionFactory connectionFactory =
				new DefaultConnectionFactory(new BrokerAddressListConfiguration(brokerAddresses));
		connectionFactory.afterPropertiesSet();
		Connection connection = connectionFactory.connect(brokerAddresses[0]);
		assertNotNull(connection);
	}

	@Test
	public void testCreateConnectionFactoryWithZookeeper() throws Exception {

		createTopic(TEST_TOPIC, 1, 1, 1);

		Partition partition = new Partition(TEST_TOPIC, 0);
		ZookeeperConnect zookeeperConnect = new ZookeeperConnect();
		zookeeperConnect.setZkConnect(kafkaEmbeddedBrokerRule.getZookeeperConnectionString());
		DefaultConnectionFactory connectionFactory =
				new DefaultConnectionFactory(new ZookeeperConfiguration(zookeeperConnect));
		connectionFactory.afterPropertiesSet();
		Connection connection = connectionFactory.connect(getKafkaRule().getBrokerAddresses()[0]);
		assertNotNull(connection);
	}

}
