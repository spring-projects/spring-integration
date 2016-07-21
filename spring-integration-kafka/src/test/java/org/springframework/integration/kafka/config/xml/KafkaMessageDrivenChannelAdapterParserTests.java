/*
 * Copyright 2015-2016 the original author or authors.
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

package org.springframework.integration.kafka.config.xml;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Collections;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.channel.NullChannel;
import org.springframework.integration.channel.PublishSubscribeChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.kafka.inbound.KafkaMessageDrivenChannelAdapter;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.listener.adapter.FilteringAcknowledgingMessageListenerAdapter;
import org.springframework.kafka.listener.adapter.RecordFilterStrategy;
import org.springframework.kafka.listener.adapter.RetryingAcknowledgingMessageListenerAdapter;
import org.springframework.kafka.listener.config.ContainerProperties;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Artem Bilan.
 * @author Gary Russell
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration
public class KafkaMessageDrivenChannelAdapterParserTests {

	@Autowired
	private NullChannel nullChannel;

	@Autowired
	private PublishSubscribeChannel errorChannel;

	@Autowired
	private KafkaMessageDrivenChannelAdapter<?, ?> kafkaListener;

	@Test
	public void testKafkaMessageDrivenChannelAdapterParser() throws Exception {
		assertThat(this.kafkaListener.isAutoStartup()).isFalse();
		assertThat(this.kafkaListener.isRunning()).isFalse();
		assertThat(this.kafkaListener.getPhase()).isEqualTo(100);
		assertThat(TestUtils.getPropertyValue(this.kafkaListener, "outputChannel")).isSameAs(this.nullChannel);
		assertThat(TestUtils.getPropertyValue(this.kafkaListener, "errorChannel")).isSameAs(this.errorChannel);
		KafkaMessageListenerContainer<?, ?> container =
				TestUtils.getPropertyValue(this.kafkaListener, "messageListenerContainer",
						KafkaMessageListenerContainer.class);
		assertThat(container).isNotNull();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testKafkaMessageDrivenChannelAdapterOptions() {
		DefaultKafkaConsumerFactory<Integer, String> cf =
				new DefaultKafkaConsumerFactory<>(Collections.<String, Object>emptyMap());
		ContainerProperties containerProps = new ContainerProperties("foo");
		KafkaMessageListenerContainer<Integer, String> container =
				new KafkaMessageListenerContainer<>(cf, containerProps);
		KafkaMessageDrivenChannelAdapter<Integer, String> adapter = new KafkaMessageDrivenChannelAdapter<>(container);
		adapter.setOutputChannel(new QueueChannel());

		adapter.setRecordFilterStrategy(mock(RecordFilterStrategy.class));
		adapter.afterPropertiesSet();

		containerProps = TestUtils.getPropertyValue(container, "containerProperties", ContainerProperties.class);

		Object messageListener = containerProps.getMessageListener();
		assertThat(messageListener).isInstanceOf(FilteringAcknowledgingMessageListenerAdapter.class);

		Object delegate = TestUtils.getPropertyValue(messageListener, "delegate");

		assertThat(delegate.getClass().getName()).contains("$IntegrationMessageListener");

		adapter.setRecordFilterStrategy(null);
		adapter.setRetryTemplate(new RetryTemplate());
		adapter.afterPropertiesSet();

		messageListener = containerProps.getMessageListener();
		assertThat(messageListener).isInstanceOf(RetryingAcknowledgingMessageListenerAdapter.class);

		delegate = TestUtils.getPropertyValue(messageListener, "delegate");

		assertThat(delegate.getClass().getName()).contains("$IntegrationMessageListener");

		adapter.setRecordFilterStrategy(mock(RecordFilterStrategy.class));
		adapter.afterPropertiesSet();

		messageListener = containerProps.getMessageListener();
		assertThat(messageListener).isInstanceOf(FilteringAcknowledgingMessageListenerAdapter.class);

		delegate = TestUtils.getPropertyValue(messageListener, "delegate");

		assertThat(delegate).isInstanceOf(RetryingAcknowledgingMessageListenerAdapter.class);

		adapter.setFilterInRetry(true);
		adapter.afterPropertiesSet();

		messageListener = containerProps.getMessageListener();
		assertThat(messageListener).isInstanceOf(RetryingAcknowledgingMessageListenerAdapter.class);

		delegate = TestUtils.getPropertyValue(messageListener, "delegate");

		assertThat(delegate).isInstanceOf(FilteringAcknowledgingMessageListenerAdapter.class);
	}

}
