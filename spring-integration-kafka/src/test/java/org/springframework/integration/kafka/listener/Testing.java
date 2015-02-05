/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.integration.kafka.listener;

import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.kafka.core.BrokerAddress;
import org.springframework.integration.kafka.core.BrokerAddressListConfiguration;
import org.springframework.integration.kafka.core.ConnectionFactory;
import org.springframework.integration.kafka.core.DefaultConnectionFactory;
import org.springframework.integration.kafka.core.Partition;
import org.springframework.integration.kafka.inbound.KafkaMessageDrivenChannelAdapter;
import org.springframework.integration.kafka.listener.AbstractMessageListenerContainerTest.KeyedMessageWithOffset;
import org.springframework.integration.kafka.serializer.common.StringDecoder;
import org.springframework.integration.kafka.support.KafkaHeaders;
import org.springframework.integration.metadata.PropertiesPersistingMetadataStore;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;

import com.gs.collections.api.multimap.list.MutableListMultimap;
import com.gs.collections.impl.multimap.list.SynchronizedPutFastListMultimap;

/**
 * @author Gary Russell
 * @since 4.1
 *
 */
public class Testing {

	@Test
	public void test() throws Exception {
		ArrayList<Partition> readPartitions = new ArrayList<Partition>();
		for (int i = 0; i < 5; i++) {
			readPartitions.add(new Partition("test-topic", i));
		}
		ConnectionFactory kafkaBrokerConnectionFactory = getKafkaBrokerConnectionFactory();
		final KafkaMessageListenerContainer kafkaMessageListenerContainer = new KafkaMessageListenerContainer(
				kafkaBrokerConnectionFactory, readPartitions.toArray(new Partition[readPartitions.size()]));
		kafkaMessageListenerContainer.setMaxFetch(100);
		kafkaMessageListenerContainer.setConcurrency(2);
		PropertiesPersistingMetadataStore metadataStore = new PropertiesPersistingMetadataStore();
		metadataStore.afterPropertiesSet();
		MetadataStoreOffsetManager offsetManager = new MetadataStoreOffsetManager(kafkaBrokerConnectionFactory);
		offsetManager.setMetadataStore(metadataStore);
		kafkaMessageListenerContainer.setOffsetManager(offsetManager);

		int expectedMessageCount = 100;

		final MutableListMultimap<Integer,KeyedMessageWithOffset> receivedData = new SynchronizedPutFastListMultimap<Integer, KeyedMessageWithOffset>();
		final CountDownLatch latch = new CountDownLatch(expectedMessageCount);

		KafkaMessageDrivenChannelAdapter kafkaInboundChannelAdapter = new KafkaMessageDrivenChannelAdapter(kafkaMessageListenerContainer);
		kafkaInboundChannelAdapter.setBeanFactory(mock(BeanFactory.class));

		StringDecoder decoder = new StringDecoder();
		kafkaInboundChannelAdapter.setKeyDecoder(decoder);
		kafkaInboundChannelAdapter.setPayloadDecoder(decoder);
		kafkaInboundChannelAdapter.setOutputChannel(new MessageChannel() {
			@Override
			public boolean send(Message<?> message) {
				latch.countDown();
				System.out.println(message);
				return receivedData.put(
						(Integer)message.getHeaders().get(KafkaHeaders.PARTITION_ID),
						new KeyedMessageWithOffset(
								(String)message.getHeaders().get(KafkaHeaders.MESSAGE_KEY),
								(String)message.getPayload(),
								(Long)message.getHeaders().get(KafkaHeaders.OFFSET),
								Thread.currentThread().getName(),
								(Integer)message.getHeaders().get(KafkaHeaders.PARTITION_ID)));
			}


			@Override
			public boolean send(Message<?> message, long timeout) {
				return send(message);
			}
		});

		kafkaInboundChannelAdapter.afterPropertiesSet();
		kafkaInboundChannelAdapter.start();

		Thread.sleep(100000);

		kafkaInboundChannelAdapter.stop();
		metadataStore.destroy();
	}

	public ConnectionFactory getKafkaBrokerConnectionFactory() throws Exception {
		DefaultConnectionFactory kafkaBrokerConnectionFactory = new DefaultConnectionFactory(
				new BrokerAddressListConfiguration(Collections.singletonList(BrokerAddress.fromAddress("localhost:9092"))));
		kafkaBrokerConnectionFactory.afterPropertiesSet();
		return kafkaBrokerConnectionFactory;
	}

}
