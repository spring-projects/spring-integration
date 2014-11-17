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

package org.springframework.integration.kafka.outbound;

import static org.junit.Assert.assertNotNull;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import kafka.admin.AdminUtils;
import kafka.consumer.ConsumerConfig;
import kafka.serializer.Decoder;
import kafka.serializer.Encoder;

import org.junit.AfterClass;
import org.junit.ClassRule;
import org.junit.Test;

import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.kafka.rule.KafkaRunning;
import org.springframework.integration.kafka.serializer.common.StringDecoder;
import org.springframework.integration.kafka.serializer.common.StringEncoder;
import org.springframework.integration.kafka.support.ConsumerConfigFactoryBean;
import org.springframework.integration.kafka.support.ConsumerConfiguration;
import org.springframework.integration.kafka.support.ConsumerConnectionProvider;
import org.springframework.integration.kafka.support.ConsumerMetadata;
import org.springframework.integration.kafka.support.KafkaConsumerContext;
import org.springframework.integration.kafka.support.KafkaHeaders;
import org.springframework.integration.kafka.support.KafkaProducerContext;
import org.springframework.integration.kafka.support.MessageLeftOverTracker;
import org.springframework.integration.kafka.support.ProducerConfiguration;
import org.springframework.integration.kafka.support.ProducerFactoryBean;
import org.springframework.integration.kafka.support.ProducerMetadata;
import org.springframework.integration.kafka.support.ZookeeperConnect;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;

/**
 * @author Gary Russell
 * @since 1.0
 *
 */
public class OutboundTests {

	private static final String TOPIC = "springintegrationtest";

	@ClassRule
	public static KafkaRunning kafkaRunning = KafkaRunning.isRunning();

	@AfterClass
	public static void tearDown() {
		try {
			AdminUtils.deleteTopic(kafkaRunning.getZkClient(), TOPIC);
		}
		catch (Exception e) {
		}
	}

	@Test
	public void testAsyncProducerFlushed() throws Exception {
		KafkaConsumerContext<String, String> consumerContext = createConsumer();
		// pre-consume to start the receiver because the high-level API doesn't support --from-beginning
		consumerContext.receive();

		KafkaProducerContext<String, String> kafkaProducerContext = new KafkaProducerContext<String, String>();
		ProducerMetadata<String, String> producerMetadata = new ProducerMetadata<String, String>(TOPIC);
		producerMetadata.setValueClassType(String.class);
		producerMetadata.setKeyClassType(String.class);
		Encoder<String> encoder = new StringEncoder<String>();
		producerMetadata.setValueEncoder(encoder);
		producerMetadata.setKeyEncoder(encoder);
		producerMetadata.setAsync(true);
		Properties props = new Properties();
		props.put("queue.buffering.max.ms", "15000");
		ProducerFactoryBean<String, String> producer =
				new ProducerFactoryBean<String, String>(producerMetadata, "localhost:9092", props);
		ProducerConfiguration<String, String> config =
				new ProducerConfiguration<String, String>(producerMetadata, producer.getObject());
		kafkaProducerContext.setProducerConfigurations(Collections.singletonMap(TOPIC, config));
		KafkaProducerMessageHandler<String, String> handler = new KafkaProducerMessageHandler<String, String>(kafkaProducerContext);

		handler.handleMessage(MessageBuilder.withPayload("foo")
				.setHeader(KafkaHeaders.MESSAGE_KEY, "3")
				.setHeader(KafkaHeaders.TOPIC, TOPIC)
				.build());

		SpelExpressionParser parser = new SpelExpressionParser();
		handler.setMessageKeyExpression(parser.parseExpression("headers.foo"));
		handler.setTopicExpression(parser.parseExpression("headers.bar"));
		StandardEvaluationContext evaluationContext = new StandardEvaluationContext();
		evaluationContext.addPropertyAccessor(new MapAccessor());
		handler.setIntegrationEvaluationContext(evaluationContext);
		handler.handleMessage(MessageBuilder.withPayload("bar")
				.setHeader("foo", "3")
				.setHeader("bar", TOPIC)
				.build());

		kafkaProducerContext.stop();

		Message<Map<String, Map<Integer, List<Object>>>> received = consumerContext.receive();
		assertNotNull(received);
		if (((Map<?,?>) received.getPayload()).size() < 2) {
			received = consumerContext.receive();
			assertNotNull(received);
		}
		consumerContext.destroy();
	}

	private KafkaConsumerContext<String, String> createConsumer() throws Exception {
		KafkaConsumerContext<String, String> consumerContext = new KafkaConsumerContext<String, String>();
		ZookeeperConnect zookeeperConnect = new ZookeeperConnect();
		zookeeperConnect.setZkConnect("localhost:2181");
		consumerContext.setZookeeperConnect(zookeeperConnect);
		ConsumerMetadata<String, String> consumerMetadata = new ConsumerMetadata<String, String>();
		consumerMetadata.setGroupId("foo");
		Decoder<String> decoder = new StringDecoder();
		consumerMetadata.setValueDecoder(decoder);
		consumerMetadata.setKeyDecoder(decoder);
		consumerMetadata.setTopicStreamMap(Collections.singletonMap(TOPIC, 1));
		Properties consumerProps = new Properties();
		consumerProps.put("consumer.timeout.ms", "500");
		ConsumerConfigFactoryBean<String, String> consumerConfigFactoryBean = new ConsumerConfigFactoryBean<String, String>(
				consumerMetadata, zookeeperConnect, consumerProps);
		ConsumerConfig consumerConfig = consumerConfigFactoryBean.getObject();
		ConsumerConnectionProvider consumerConnectionProvider = new ConsumerConnectionProvider(consumerConfig);
		MessageLeftOverTracker<String, String> messageLeftOverTracker = new MessageLeftOverTracker<String, String>();
		ConsumerConfiguration<String, String> cConfig = new ConsumerConfiguration<String, String>(consumerMetadata,
				consumerConnectionProvider, messageLeftOverTracker);
		cConfig.setMaxMessages(1);
		consumerContext.setConsumerConfigurations(Collections.singletonMap("foo", cConfig));
		return consumerContext;
	}

}
