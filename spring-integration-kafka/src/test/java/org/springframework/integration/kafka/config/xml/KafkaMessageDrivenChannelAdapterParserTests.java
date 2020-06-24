/*
 * Copyright 2015-2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.kafka.config.xml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Collections;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.kafka.inbound.KafkaMessageDrivenChannelAdapter;
import org.springframework.integration.kafka.inbound.KafkaMessageDrivenChannelAdapter.ListenerMode;
import org.springframework.integration.support.ErrorMessageStrategy;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.adapter.FilteringMessageListenerAdapter;
import org.springframework.kafka.listener.adapter.RecordFilterStrategy;
import org.springframework.kafka.listener.adapter.RetryingMessageListenerAdapter;
import org.springframework.retry.RecoveryCallback;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

/**
 * @author Artem Bilan.
 * @author Gary Russell
 *
 * @since 5.4
 */
@SpringJUnitConfig
@DirtiesContext
class KafkaMessageDrivenChannelAdapterParserTests {

	@Autowired
	private NullChannel nullChannel;

	@Autowired
	private PublishSubscribeChannel errorChannel;

	@Autowired
	private KafkaMessageDrivenChannelAdapter<?, ?> kafkaListener;

	@Autowired
	private KafkaMessageDrivenChannelAdapter<?, ?> kafkaBatchListener;

	@Autowired
	private ErrorMessageStrategy ems;

	@Autowired
	private RetryTemplate retryTemplate;

	@Autowired
	private RecoveryCallback<?> recoveryCallback;

	@Test
	void testKafkaMessageDrivenChannelAdapterParser() {
		assertThat(this.kafkaListener.isAutoStartup()).isFalse();
		assertThat(this.kafkaListener.isRunning()).isFalse();
		assertThat(this.kafkaListener.getPhase()).isEqualTo(100);
		assertThat(TestUtils.getPropertyValue(this.kafkaListener, "outputChannel")).isSameAs(this.nullChannel);
		KafkaMessageListenerContainer<?, ?> container =
				TestUtils.getPropertyValue(this.kafkaListener, "messageListenerContainer",
						KafkaMessageListenerContainer.class);
		assertThat(container).isNotNull();
		assertThat(TestUtils.getPropertyValue(this.kafkaListener, "mode", ListenerMode.class))
				.isEqualTo(ListenerMode.record);
		assertThat(TestUtils.getPropertyValue(this.kafkaListener, "recordListener.fallbackType"))
				.isEqualTo(String.class);
		assertThat(TestUtils.getPropertyValue(this.kafkaListener, "batchListener.fallbackType"))
				.isEqualTo(String.class);
		assertThat(TestUtils.getPropertyValue(this.kafkaListener, "errorMessageStrategy")).isSameAs(this.ems);
		assertThat(TestUtils.getPropertyValue(this.kafkaListener, "retryTemplate")).isSameAs(this.retryTemplate);
		assertThat(TestUtils.getPropertyValue(this.kafkaListener, "recoveryCallback")).isSameAs(this.recoveryCallback);
	}

	@Test
	void testKafkaBatchMessageDrivenChannelAdapterParser() {
		assertThat(this.kafkaBatchListener.isAutoStartup()).isFalse();
		assertThat(this.kafkaBatchListener.isRunning()).isFalse();
		assertThat(this.kafkaBatchListener.getPhase()).isEqualTo(100);
		assertThat(TestUtils.getPropertyValue(this.kafkaBatchListener, "outputChannel")).isSameAs(this.nullChannel);
		assertThat(TestUtils.getPropertyValue(this.kafkaBatchListener, "errorChannel")).isSameAs(this.errorChannel);
		KafkaMessageListenerContainer<?, ?> container =
				TestUtils.getPropertyValue(this.kafkaBatchListener, "messageListenerContainer",
						KafkaMessageListenerContainer.class);
		assertThat(container).isNotNull();
		assertThat(TestUtils.getPropertyValue(this.kafkaBatchListener, "mode", ListenerMode.class))
				.isEqualTo(ListenerMode.batch);
	}

	@Test
	@SuppressWarnings("unchecked")
	void testKafkaMessageDrivenChannelAdapterOptions() {
		DefaultKafkaConsumerFactory<Integer, String> cf =
				new DefaultKafkaConsumerFactory<>(Collections.emptyMap());
		ContainerProperties containerProps = new ContainerProperties("foo");
		KafkaMessageListenerContainer<Integer, String> container =
				new KafkaMessageListenerContainer<>(cf, containerProps);
		KafkaMessageDrivenChannelAdapter<Integer, String> adapter = new KafkaMessageDrivenChannelAdapter<>(container);
		adapter.setOutputChannel(new QueueChannel());

		adapter.setRecordFilterStrategy(mock(RecordFilterStrategy.class));
		adapter.afterPropertiesSet();

		containerProps = TestUtils.getPropertyValue(container, "containerProperties", ContainerProperties.class);

		Object messageListener = containerProps.getMessageListener();
		assertThat(messageListener).isInstanceOf(FilteringMessageListenerAdapter.class);

		Object delegate = TestUtils.getPropertyValue(messageListener, "delegate");

		assertThat(delegate.getClass().getName()).contains("$IntegrationRecordMessageListener");

		adapter.setRecordFilterStrategy(null);
		adapter.setRetryTemplate(new RetryTemplate());
		adapter.afterPropertiesSet();

		messageListener = containerProps.getMessageListener();
		assertThat(messageListener).isInstanceOf(RetryingMessageListenerAdapter.class);

		delegate = TestUtils.getPropertyValue(messageListener, "delegate");

		assertThat(delegate.getClass().getName()).contains("$IntegrationRecordMessageListener");

		adapter.setRecordFilterStrategy(mock(RecordFilterStrategy.class));
		adapter.afterPropertiesSet();

		messageListener = containerProps.getMessageListener();
		assertThat(messageListener).isInstanceOf(FilteringMessageListenerAdapter.class);

		delegate = TestUtils.getPropertyValue(messageListener, "delegate");

		assertThat(delegate).isInstanceOf(RetryingMessageListenerAdapter.class);

		adapter.setFilterInRetry(true);
		adapter.afterPropertiesSet();

		messageListener = containerProps.getMessageListener();
		assertThat(messageListener).isInstanceOf(RetryingMessageListenerAdapter.class);

		delegate = TestUtils.getPropertyValue(messageListener, "delegate");

		assertThat(delegate).isInstanceOf(FilteringMessageListenerAdapter.class);
	}

}
