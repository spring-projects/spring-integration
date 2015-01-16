/*
 * Copyright 2015 the original author or authors.
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

import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import kafka.admin.AdminUtils;
import kafka.api.OffsetRequest;
import kafka.common.TopicExistsException;
import kafka.serializer.Decoder;
import kafka.serializer.Encoder;
import org.junit.AfterClass;
import org.junit.ClassRule;
import org.junit.Test;

import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.integration.kafka.core.DefaultConnectionFactory;
import org.springframework.integration.kafka.core.KafkaMessage;
import org.springframework.integration.kafka.core.ZookeeperConfiguration;
import org.springframework.integration.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.integration.kafka.listener.MessageListener;
import org.springframework.integration.kafka.listener.MetadataStoreOffsetManager;
import org.springframework.integration.kafka.rule.KafkaRunning;
import org.springframework.integration.kafka.serializer.common.StringDecoder;
import org.springframework.integration.kafka.serializer.common.StringEncoder;
import org.springframework.integration.kafka.support.KafkaHeaders;
import org.springframework.integration.kafka.support.KafkaProducerContext;
import org.springframework.integration.kafka.support.ProducerConfiguration;
import org.springframework.integration.kafka.support.ProducerFactoryBean;
import org.springframework.integration.kafka.support.ProducerMetadata;
import org.springframework.integration.kafka.support.ZookeeperConnect;
import org.springframework.integration.kafka.util.MessageUtils;
import org.springframework.messaging.support.MessageBuilder;

/**
 * @author Gary Russell
 * @author Marius Bogoevici
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

		// create the topic

		try {
			AdminUtils.createTopic(kafkaRunning.getZkClient(), TOPIC, 1, 1, new Properties());
		}
		catch (TopicExistsException e) {
			// do nothing
		}

		final String suffix = UUID.randomUUID().toString();

		ZookeeperConfiguration configuration = new ZookeeperConfiguration(new ZookeeperConnect());
		DefaultConnectionFactory connectionFactory = new DefaultConnectionFactory(configuration);
		connectionFactory.afterPropertiesSet();
		final KafkaMessageListenerContainer kafkaMessageListenerContainer = new KafkaMessageListenerContainer(connectionFactory, TOPIC);
		kafkaMessageListenerContainer.setMaxFetch(100);
		kafkaMessageListenerContainer.setConcurrency(1);
		MetadataStoreOffsetManager offsetManager = new MetadataStoreOffsetManager(connectionFactory);
		// start reading at the end of the
		offsetManager.setReferenceTimestamp(OffsetRequest.LatestTime());
		kafkaMessageListenerContainer.setOffsetManager(offsetManager);
		final Decoder<String> decoder = new StringDecoder();

		int expectedMessageCount = 2;
		final List<String> payloads = new ArrayList<String>();
		final CountDownLatch latch = new CountDownLatch(expectedMessageCount);
		kafkaMessageListenerContainer.setMessageListener(new MessageListener() {
			@Override
			public void onMessage(KafkaMessage message) {
				payloads.add(MessageUtils.decodePayload(message, decoder));
				latch.countDown();
			}
		});

		kafkaMessageListenerContainer.start();

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

		handler.handleMessage(MessageBuilder.withPayload("foo"+suffix)
				.setHeader(KafkaHeaders.MESSAGE_KEY, "3")
				.setHeader(KafkaHeaders.TOPIC, TOPIC)
				.build());

		SpelExpressionParser parser = new SpelExpressionParser();
		handler.setMessageKeyExpression(parser.parseExpression("headers.foo"));
		handler.setTopicExpression(parser.parseExpression("headers.bar"));
		StandardEvaluationContext evaluationContext = new StandardEvaluationContext();
		evaluationContext.addPropertyAccessor(new MapAccessor());
		handler.setIntegrationEvaluationContext(evaluationContext);
		handler.handleMessage(MessageBuilder.withPayload("bar"+suffix)
				.setHeader("foo", "3")
				.setHeader("bar", TOPIC)
				.build());

		kafkaProducerContext.stop();

		latch.await(1000, TimeUnit.MILLISECONDS);
		assertThat(latch.getCount(), equalTo(0L));
		for (String payload : payloads) {
			assertThat(payload, endsWith(suffix));
		}
		kafkaMessageListenerContainer.stop();
	}

}
