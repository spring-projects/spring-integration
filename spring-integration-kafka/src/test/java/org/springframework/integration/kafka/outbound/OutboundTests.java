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

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.integration.kafka.core.DefaultConnectionFactory;
import org.springframework.integration.kafka.core.KafkaMessage;
import org.springframework.integration.kafka.core.ZookeeperConfiguration;
import org.springframework.integration.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.integration.kafka.listener.MessageListener;
import org.springframework.integration.kafka.listener.MetadataStoreOffsetManager;
import org.springframework.integration.kafka.rule.KafkaEmbedded;
import org.springframework.integration.kafka.rule.KafkaRule;
import org.springframework.integration.kafka.serializer.common.StringDecoder;
import org.springframework.integration.kafka.serializer.common.StringEncoder;
import org.springframework.integration.kafka.support.KafkaHeaders;
import org.springframework.integration.kafka.support.KafkaProducerContext;
import org.springframework.integration.kafka.support.ProducerConfiguration;
import org.springframework.integration.kafka.support.ProducerFactoryBean;
import org.springframework.integration.kafka.support.ProducerListener;
import org.springframework.integration.kafka.support.ProducerMetadata;
import org.springframework.integration.kafka.support.ZookeeperConnect;
import org.springframework.integration.kafka.util.EncoderAdaptingSerializer;
import org.springframework.integration.kafka.util.MessageUtils;
import org.springframework.integration.kafka.util.TopicUtils;
import org.springframework.messaging.support.MessageBuilder;

import com.gs.collections.api.multimap.MutableMultimap;
import com.gs.collections.impl.factory.Multimaps;

import kafka.admin.AdminUtils;
import kafka.api.OffsetRequest;
import kafka.common.TopicExistsException;
import kafka.serializer.Decoder;
import kafka.serializer.Encoder;

/**
 * @author Gary Russell
 * @author Marius Bogoevici
 * @since 1.0
 */
public class OutboundTests {

	private static final String TOPIC = "springintegrationtest";

	private static final String TOPIC2 = "springintegrationtest2";

	@Rule
	public KafkaRule kafkaRule = new KafkaEmbedded(1);

	private final Decoder<String> decoder = new StringDecoder();

	@After
	public void tearDown() {
		try {
			AdminUtils.deleteTopic(kafkaRule.getZkClient(), TOPIC);
			AdminUtils.deleteTopic(kafkaRule.getZkClient(), TOPIC2);
		}
		catch (Exception e) {
		}
	}

	@Test
	public void testAsyncProducerFlushed() throws Exception {

		// create the topic

		try {
			TopicUtils.ensureTopicCreated(kafkaRule.getZookeeperConnectionString(), TOPIC, 1, 1);
		}
		catch (TopicExistsException e) {
			// do nothing
		}

		final String suffix = UUID.randomUUID().toString();

		KafkaMessageListenerContainer kafkaMessageListenerContainer = createMessageListenerContainer(TOPIC);

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

		KafkaProducerContext producerContext = createProducerContext();
		KafkaProducerMessageHandler handler =
				new KafkaProducerMessageHandler(producerContext);

		handler.handleMessage(MessageBuilder.withPayload("foo" + suffix)
				.setHeader(KafkaHeaders.MESSAGE_KEY, "3")
				.setHeader(KafkaHeaders.TOPIC, TOPIC)
				.build());

		SpelExpressionParser parser = new SpelExpressionParser();
		handler.setMessageKeyExpression(parser.parseExpression("headers.foo"));
		handler.setTopicExpression(parser.parseExpression("headers.bar"));
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();

		handler.handleMessage(MessageBuilder.withPayload("bar" + suffix)
				.setHeader("foo", "3")
				.setHeader("bar", TOPIC)
				.build());

		producerContext.stop();

		latch.await(1000, TimeUnit.MILLISECONDS);
		assertThat(latch.getCount(), equalTo(0L));
		for (String payload : payloads) {
			assertThat(payload, endsWith(suffix));
		}
		kafkaMessageListenerContainer.stop();
	}

	@Test
	public void testHeaderRoutingAndAsyncCallback() throws Exception {

		// create the topic

		try {
			TopicUtils.ensureTopicCreated(kafkaRule.getZookeeperConnectionString(), TOPIC, 1, 1);
		}
		catch (TopicExistsException e) {
			// do nothing
		}

		try {
			TopicUtils.ensureTopicCreated(kafkaRule.getZookeeperConnectionString(), TOPIC2, 1, 1);
		}
		catch (TopicExistsException e) {
			// do nothing
		}

		final String suffix = UUID.randomUUID().toString();

		KafkaMessageListenerContainer kafkaMessageListenerContainer = createMessageListenerContainer(TOPIC, TOPIC2);

		final Decoder<String> decoder = new StringDecoder();

		int expectedMessageCount = 4;
		final MutableMultimap<String, String> payloadsByTopic = Multimaps.mutable.list.with();
		final CountDownLatch latch = new CountDownLatch(expectedMessageCount);
		kafkaMessageListenerContainer.setMessageListener(new MessageListener() {

			@Override
			public void onMessage(KafkaMessage message) {
				payloadsByTopic.put(message.getMetadata().getPartition().getTopic(),
						MessageUtils.decodePayload(message, decoder));
				latch.countDown();
			}

		});

		kafkaMessageListenerContainer.start();

		int expectedDeliveryConfirmations = 4;
		final List<RecordMetadata> results = new ArrayList<RecordMetadata>();
		final CountDownLatch sendResultLatch = new CountDownLatch(expectedDeliveryConfirmations);
		ProducerListener listener = new ProducerListener() {

			@Override
			public void onSuccess(String topic, Integer partition, Object key, Object value, RecordMetadata recordMetadata) {
				results.add(recordMetadata);
				sendResultLatch.countDown();
			}

			@Override
			public void onError(String topic, Integer partition, Object key, Object value, Exception exception) {
				sendResultLatch.countDown();
			}
		};

		KafkaProducerContext producerContext = createProducerContext(listener);
		KafkaProducerMessageHandler handler
				= new KafkaProducerMessageHandler(producerContext);

		handler.handleMessage(MessageBuilder.withPayload("fooTopic1" + suffix)
				.setHeader(KafkaHeaders.MESSAGE_KEY, "3")
				.setHeader(KafkaHeaders.TOPIC, TOPIC)
				.build());

		handler.handleMessage(MessageBuilder.withPayload("fooTopic2" + suffix)
				.setHeader(KafkaHeaders.MESSAGE_KEY, "3")
				.setHeader(KafkaHeaders.TOPIC, TOPIC2)
				.build());

		SpelExpressionParser parser = new SpelExpressionParser();
		handler.setMessageKeyExpression(parser.parseExpression("headers.foo"));
		handler.setTopicExpression(parser.parseExpression("headers.bar"));
		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();

		handler.handleMessage(MessageBuilder.withPayload("bar1" + suffix)
				.setHeader("foo", "3")
				.setHeader("bar", TOPIC)
				.build());

		handler.handleMessage(MessageBuilder.withPayload("bar2" + suffix)
				.setHeader("foo", "3")
				.setHeader("bar", TOPIC2)
				.build());

		assertTrue(sendResultLatch.await(10, TimeUnit.SECONDS));
		assertThat(results.size(), equalTo(expectedDeliveryConfirmations));

		producerContext.stop();

		latch.await(10000, TimeUnit.MILLISECONDS);
		assertThat(latch.getCount(), equalTo(0L));
		// messages are routed to both topics
		assertThat(payloadsByTopic.keysView(), hasItem(TOPIC));
		assertThat(payloadsByTopic.keysView(), hasItem(TOPIC2));
		assertThat(payloadsByTopic.toMap().get(TOPIC), contains("fooTopic1" + suffix, "bar1" + suffix));
		assertThat(payloadsByTopic.toMap().get(TOPIC2), contains("fooTopic2" + suffix, "bar2" + suffix));

		kafkaMessageListenerContainer.stop();
	}


	@Test
	public void testNoHeader() throws Exception {

		// create the topic

		try {
			TopicUtils.ensureTopicCreated(kafkaRule.getZookeeperConnectionString(), TOPIC, 1, 1);
		}
		catch (TopicExistsException e) {
			// do nothing
		}

		final String suffix = UUID.randomUUID().toString();

		KafkaMessageListenerContainer kafkaMessageListenerContainer = createMessageListenerContainer(TOPIC);

		final Decoder<String> decoder = new StringDecoder();

		int expectedMessageCount = 1;
		final MutableMultimap<String, String> payloadsByTopic = Multimaps.mutable.list.with();
		final CountDownLatch latch = new CountDownLatch(expectedMessageCount);
		kafkaMessageListenerContainer.setMessageListener(new MessageListener() {

			@Override
			public void onMessage(KafkaMessage message) {
				payloadsByTopic.put(message.getMetadata().getPartition().getTopic(),
						MessageUtils.decodePayload(message, decoder));
				latch.countDown();
			}

		});

		kafkaMessageListenerContainer.start();

		KafkaProducerContext producerContext = createProducerContext();
		KafkaProducerMessageHandler handler = new KafkaProducerMessageHandler(producerContext);

		handler.setBeanFactory(mock(BeanFactory.class));
		handler.afterPropertiesSet();

		handler.handleMessage(MessageBuilder.withPayload("fooTopic1" + suffix).build());

		producerContext.stop();

		latch.await(1000, TimeUnit.MILLISECONDS);
		assertThat(latch.getCount(), equalTo(0L));
		assertThat(payloadsByTopic.keysView(), hasItem(TOPIC));
		assertThat(payloadsByTopic.toMap().get(TOPIC).toList(), hasSize(1));
		assertThat(payloadsByTopic.toMap().get(TOPIC), contains("fooTopic1" + suffix));
		kafkaMessageListenerContainer.stop();
	}

	private KafkaMessageListenerContainer createMessageListenerContainer(String... topics) throws Exception {
		ZookeeperConfiguration configuration =
				new ZookeeperConfiguration(new ZookeeperConnect(kafkaRule.getZookeeperConnectionString()));
		DefaultConnectionFactory connectionFactory = new DefaultConnectionFactory(configuration);
		connectionFactory.afterPropertiesSet();
		final KafkaMessageListenerContainer kafkaMessageListenerContainer =
				new KafkaMessageListenerContainer(connectionFactory, topics);
		kafkaMessageListenerContainer.setMaxFetch(100);
		kafkaMessageListenerContainer.setConcurrency(1);
		MetadataStoreOffsetManager offsetManager = new MetadataStoreOffsetManager(connectionFactory);
		// start reading at the end of the
		offsetManager.setReferenceTimestamp(OffsetRequest.LatestTime());
		kafkaMessageListenerContainer.setOffsetManager(offsetManager);
		return kafkaMessageListenerContainer;
	}

	private KafkaProducerContext createProducerContext() throws Exception {
		return createProducerContext(null);
	}

	private KafkaProducerContext createProducerContext(ProducerListener producerListener) throws Exception {
		KafkaProducerContext kafkaProducerContext = new KafkaProducerContext();
		Encoder<String> encoder = new StringEncoder();
		ProducerMetadata<String, String> producerMetadata =
				new ProducerMetadata<String, String>(TOPIC, String.class, String.class,
						new EncoderAdaptingSerializer<>(encoder), new EncoderAdaptingSerializer<>(encoder));
		Properties props = new Properties();
		if (producerListener == null) {
			props.put("linger.ms", "15000");
		}
		ProducerFactoryBean<String, String> producer =
				new ProducerFactoryBean<>(producerMetadata, kafkaRule.getBrokersAsString(), props);
		ProducerConfiguration<String, String> config =
				new ProducerConfiguration<>(producerMetadata, producer.getObject());
		config.setProducerListener(producerListener);
		Map<String, ProducerConfiguration<?, ?>> producerConfigurationMap =
				Collections.<String, ProducerConfiguration<?, ?>>singletonMap(TOPIC, config);
		kafkaProducerContext.setProducerConfigurations(producerConfigurationMap);
		return kafkaProducerContext;
	}

}
