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

import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.junit.Rule;
import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.channel.FixedSubscriberChannel;
import org.springframework.integration.kafka.core.ConnectionFactory;
import org.springframework.integration.kafka.core.Partition;
import org.springframework.integration.kafka.inbound.KafkaMessageDrivenChannelAdapter;
import org.springframework.integration.kafka.rule.KafkaEmbedded;
import org.springframework.integration.kafka.rule.KafkaRule;
import org.springframework.integration.kafka.serializer.common.StringDecoder;
import org.springframework.integration.kafka.support.KafkaHeaders;
import org.springframework.integration.kafka.support.ZookeeperConnect;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessagingException;

import com.gs.collections.api.multimap.list.MutableListMultimap;
import com.gs.collections.impl.multimap.list.SynchronizedPutFastListMultimap;

import kafka.message.NoCompressionCodec$;

/**
 * @author Marius Bogoevici
 */
public class KafkaMessageDrivenChannelAdapterWithKafkaOffsetManagerTests extends AbstractMessageListenerContainerTests {

	@Rule
	public final KafkaEmbedded kafkaEmbeddedBrokerRule = new KafkaEmbedded(1);

	@Override
	public KafkaRule getKafkaRule() {
		return kafkaEmbeddedBrokerRule;
	}

	@Test
	public void testKafkaOffsetManager() throws Exception {
		createTopic(TEST_TOPIC, 5, 1, 1);

		ConnectionFactory connectionFactory = getKafkaBrokerConnectionFactory();
		ArrayList<Partition> readPartitions = new ArrayList<Partition>();
		for (int i = 0; i < 5; i++) {
			readPartitions.add(new Partition(TEST_TOPIC, i));
		}

		final KafkaMessageListenerContainer kafkaMessageListenerContainer =
				new KafkaMessageListenerContainer(connectionFactory,
						readPartitions.toArray(new Partition[readPartitions.size()]));
		KafkaTopicOffsetManager offsetManager = new KafkaTopicOffsetManager(
				new ZookeeperConnect(getKafkaRule().getZookeeperConnectionString()), "si-offsets");
		offsetManager.afterPropertiesSet();
		kafkaMessageListenerContainer.setOffsetManager(
				offsetManager);
		kafkaMessageListenerContainer.setMaxFetch(100);
		kafkaMessageListenerContainer.setConcurrency(2);

		int expectedMessageCount = 100;

		final MutableListMultimap<Integer, KeyedMessageWithOffset> receivedData =
				new SynchronizedPutFastListMultimap<Integer, KeyedMessageWithOffset>();
		final CountDownLatch latch = new CountDownLatch(expectedMessageCount);

		KafkaMessageDrivenChannelAdapter kafkaMessageDrivenChannelAdapter =
				new KafkaMessageDrivenChannelAdapter(kafkaMessageListenerContainer);

		StringDecoder decoder = new StringDecoder();
		kafkaMessageDrivenChannelAdapter.setKeyDecoder(decoder);
		kafkaMessageDrivenChannelAdapter.setPayloadDecoder(decoder);
		kafkaMessageDrivenChannelAdapter.setBeanFactory(mock(BeanFactory.class));
		kafkaMessageDrivenChannelAdapter.setOutputChannel(new FixedSubscriberChannel(new MessageHandler() {

			@Override
			public void handleMessage(Message<?> message) throws MessagingException {
				receivedData.put(
						(Integer) message.getHeaders().get(KafkaHeaders.PARTITION_ID),
						new KeyedMessageWithOffset(
								(String) message.getHeaders().get(KafkaHeaders.MESSAGE_KEY),
								(String) message.getPayload(),
								(Long) message.getHeaders().get(KafkaHeaders.OFFSET),
								Thread.currentThread().getName(),
								(Integer) message.getHeaders().get(KafkaHeaders.PARTITION_ID)));
				latch.countDown();
			}

		}));

		kafkaMessageDrivenChannelAdapter.afterPropertiesSet();
		kafkaMessageDrivenChannelAdapter.start();

		createStringProducer(NoCompressionCodec$.MODULE$.codec()).send(createMessages(expectedMessageCount, TEST_TOPIC));

		Thread.sleep(100);

		kafkaMessageDrivenChannelAdapter.stop();
		kafkaMessageDrivenChannelAdapter.start();

		latch.await((expectedMessageCount / 5000) + 1, TimeUnit.MINUTES);
		kafkaMessageListenerContainer.stop();

		assertThat(receivedData.valuesView().toList(), hasSize(expectedMessageCount));
		assertThat(latch.getCount(), equalTo(0L));
		System.out.println("All messages received ... checking ");

		validateMessageReceipt(receivedData, 2, 5, 100, expectedMessageCount, readPartitions, 1);

	}

}
