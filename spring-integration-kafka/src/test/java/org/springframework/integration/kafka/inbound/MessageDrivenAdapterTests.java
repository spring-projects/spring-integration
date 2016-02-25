/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.integration.kafka.inbound;

import static org.junit.Assert.assertNotNull;

import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.junit.ClassRule;
import org.junit.Test;

import org.springframework.integration.channel.QueueChannel;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.test.rule.KafkaEmbedded;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.messaging.Message;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @since 2.0
 *
 */
public class MessageDrivenAdapterTests {

	private static String topic1 = "testTopic1";

	private static String topic2 = "testTopic2";

	@ClassRule
	public static KafkaEmbedded embeddedKafka = new KafkaEmbedded(1, true, topic1, topic2);

	@Test
	public void testInbound() throws Exception {
		Map<String, Object> props = KafkaTestUtils.consumerProps("test1", "true", embeddedKafka);
		props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
		DefaultKafkaConsumerFactory<Integer, String> cf = new DefaultKafkaConsumerFactory<Integer, String>(props);
		KafkaMessageListenerContainer<Integer, String> container =
				new KafkaMessageListenerContainer<>(cf, topic1);
		KafkaMessageDrivenChannelAdapter<Integer, String> adapter = new KafkaMessageDrivenChannelAdapter<>(container);
		QueueChannel out = new QueueChannel();
		adapter.setOutputChannel(out);
		adapter.afterPropertiesSet();
		adapter.start();

		Map<String, Object> senderProps = KafkaTestUtils.producerProps(embeddedKafka);
		ProducerFactory<Integer, String> pf = new DefaultKafkaProducerFactory<Integer, String>(senderProps);
		KafkaTemplate<Integer, String> template = new KafkaTemplate<>(pf);
		template.setDefaultTopic("testTopic1");
		template.convertAndSend("foo");
		Message<?> received = out.receive(10000);
		assertNotNull(received);
		adapter.stop();
	}

}
