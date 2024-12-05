/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.amqp.config;

import org.junit.jupiter.api.Test;

import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.listener.AbstractMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.DirectMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.parsing.BeanDefinitionParsingException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.amqp.inbound.AmqpInboundChannelAdapter;
import org.springframework.integration.amqp.inbound.AmqpInboundChannelAdapter.BatchMode;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 2.1
 */
@SpringJUnitConfig
@DirtiesContext
public class AmqpInboundChannelAdapterParserTests {

	@Autowired
	private ApplicationContext context;

	@Test
	public void verifyIdAsChannel() {
		Object channel = context.getBean("rabbitInbound");
		Object adapter = context.getBean("rabbitInbound.adapter");
		assertThat(channel.getClass()).isEqualTo(DirectChannel.class);
		assertThat(adapter.getClass()).isEqualTo(AmqpInboundChannelAdapter.class);
		assertThat(TestUtils.getPropertyValue(adapter, "autoStartup")).isEqualTo(Boolean.TRUE);
		assertThat(TestUtils.getPropertyValue(adapter, "phase")).isEqualTo(Integer.MAX_VALUE / 2);
		assertThat(TestUtils.getPropertyValue(adapter, "messageListenerContainer.missingQueuesFatal", Boolean.class))
				.isTrue();
		assertThat(TestUtils.getPropertyValue(adapter, "messageListenerContainer"))
				.isInstanceOf(SimpleMessageListenerContainer.class);
		assertThat(TestUtils.getPropertyValue(adapter, "batchMode", BatchMode.class))
				.isEqualTo(BatchMode.EXTRACT_PAYLOADS);
		assertThat(TestUtils.getPropertyValue(adapter, "messageListenerContainer.batchSize", Integer.class))
				.isEqualTo(2);
	}

	@Test
	public void verifyDMCC() {
		Object adapter = context.getBean("dmlc.adapter");
		assertThat(adapter.getClass()).isEqualTo(AmqpInboundChannelAdapter.class);
		assertThat(TestUtils.getPropertyValue(adapter, "messageListenerContainer.missingQueuesFatal", Boolean.class))
				.isFalse();
		assertThat(TestUtils.getPropertyValue(adapter, "messageListenerContainer"))
				.isInstanceOf(DirectMessageListenerContainer.class);
		assertThat(TestUtils.getPropertyValue(adapter, "messageListenerContainer.consumersPerQueue")).isEqualTo(2);
		assertThat(TestUtils.getPropertyValue(adapter, "batchMode", BatchMode.class))
				.isEqualTo(BatchMode.MESSAGES);
	}

	@Test
	public void verifyLifeCycle() {
		Object adapter = context.getBean("autoStartFalse.adapter");
		assertThat(TestUtils.getPropertyValue(adapter, "autoStartup")).isEqualTo(Boolean.FALSE);
		assertThat(TestUtils.getPropertyValue(adapter, "phase")).isEqualTo(123);
		assertThat(TestUtils.getPropertyValue(adapter, "messageListenerContainer.acknowledgeMode"))
				.isEqualTo(AcknowledgeMode.NONE);
		assertThat(TestUtils.getPropertyValue(adapter, "messageListenerContainer.missingQueuesFatal", Boolean.class))
				.isFalse();
		assertThat(TestUtils.getPropertyValue(adapter, "messageListenerContainer.batchSize", Integer.class))
				.isEqualTo(3);
	}

	@Test
	public void withHeaderMapperStandardAndCustomHeaders() throws Exception {
		AmqpInboundChannelAdapter adapter = context.getBean("withHeaderMapperStandardAndCustomHeaders",
				AmqpInboundChannelAdapter.class);

		AbstractMessageListenerContainer mlc =
				TestUtils.getPropertyValue(adapter, "messageListenerContainer", AbstractMessageListenerContainer.class);
		ChannelAwareMessageListener listener = TestUtils.getPropertyValue(mlc, "messageListener",
				ChannelAwareMessageListener.class);
		MessageProperties amqpProperties = new MessageProperties();
		amqpProperties.setAppId("test.appId");
		amqpProperties.setClusterId("test.clusterId");
		amqpProperties.setContentEncoding("test.contentEncoding");
		amqpProperties.setContentLength(99L);
		amqpProperties.setContentType("test.contentType");
		amqpProperties.setHeader("foo", "foo");
		amqpProperties.setHeader("bar", "bar");
		Message amqpMessage = new Message("hello".getBytes(), amqpProperties);
		listener.onMessage(amqpMessage, null);
		QueueChannel requestChannel = context.getBean("requestChannel", QueueChannel.class);
		org.springframework.messaging.Message<?> siMessage = requestChannel.receive(0);
		assertThat(siMessage.getHeaders().get("foo")).isEqualTo("foo");
		assertThat(siMessage.getHeaders().get("bar")).isNull();
		assertThat(siMessage.getHeaders().get(AmqpHeaders.CONTENT_ENCODING)).isNotNull();
		assertThat(siMessage.getHeaders().get(AmqpHeaders.CLUSTER_ID)).isNotNull();
		assertThat(siMessage.getHeaders().get(AmqpHeaders.APP_ID)).isNotNull();
		assertThat(siMessage.getHeaders().get(AmqpHeaders.CONTENT_TYPE)).isNotNull();
	}

	@Test
	public void withHeaderMapperOnlyCustomHeaders() throws Exception {
		AmqpInboundChannelAdapter adapter = context.getBean("withHeaderMapperOnlyCustomHeaders",
				AmqpInboundChannelAdapter.class);

		AbstractMessageListenerContainer mlc =
				TestUtils.getPropertyValue(adapter, "messageListenerContainer", AbstractMessageListenerContainer.class);
		ChannelAwareMessageListener listener = TestUtils.getPropertyValue(mlc, "messageListener",
				ChannelAwareMessageListener.class);
		MessageProperties amqpProperties = new MessageProperties();
		amqpProperties.setAppId("test.appId");
		amqpProperties.setClusterId("test.clusterId");
		amqpProperties.setContentEncoding("test.contentEncoding");
		amqpProperties.setContentLength(99L);
		amqpProperties.setContentType("test.contentType");
		amqpProperties.setHeader("foo", "foo");
		amqpProperties.setHeader("bar", "bar");
		Message amqpMessage = new Message("hello".getBytes(), amqpProperties);
		listener.onMessage(amqpMessage, null);
		QueueChannel requestChannel = context.getBean("requestChannel", QueueChannel.class);
		org.springframework.messaging.Message<?> siMessage = requestChannel.receive(0);
		assertThat(siMessage.getHeaders().get("foo")).isEqualTo("foo");
		assertThat(siMessage.getHeaders().get("bar")).isNull();
		assertThat(siMessage.getHeaders().get(AmqpHeaders.CONTENT_ENCODING)).isNull();
		assertThat(siMessage.getHeaders().get(AmqpHeaders.CLUSTER_ID)).isNull();
		assertThat(siMessage.getHeaders().get(AmqpHeaders.APP_ID)).isNull();
		assertThat(siMessage.getHeaders().get(AmqpHeaders.CONTENT_TYPE)).isNull();
	}

	@Test
	public void withHeaderMapperNothingToMap() throws Exception {
		AmqpInboundChannelAdapter adapter = context.getBean("withHeaderMapperNothingToMap",
				AmqpInboundChannelAdapter.class);

		AbstractMessageListenerContainer mlc =
				TestUtils.getPropertyValue(adapter, "messageListenerContainer", AbstractMessageListenerContainer.class);
		ChannelAwareMessageListener listener = TestUtils.getPropertyValue(mlc, "messageListener",
				ChannelAwareMessageListener.class);
		MessageProperties amqpProperties = new MessageProperties();
		amqpProperties.setAppId("test.appId");
		amqpProperties.setClusterId("test.clusterId");
		amqpProperties.setContentEncoding("test.contentEncoding");
		amqpProperties.setContentLength(99L);
		amqpProperties.setContentType("test.contentType");
		amqpProperties.setHeader("foo", "foo");
		amqpProperties.setHeader("bar", "bar");
		Message amqpMessage = new Message("hello".getBytes(), amqpProperties);
		listener.onMessage(amqpMessage, null);

		QueueChannel requestChannel = context.getBean("requestChannel", QueueChannel.class);
		org.springframework.messaging.Message<?> siMessage = requestChannel.receive(0);
		assertThat(siMessage.getHeaders().get("foo")).isNull();
		assertThat(siMessage.getHeaders().get("bar")).isNull();
		assertThat(siMessage.getHeaders().get(AmqpHeaders.CONTENT_ENCODING)).isNull();
		assertThat(siMessage.getHeaders().get(AmqpHeaders.CLUSTER_ID)).isNull();
		assertThat(siMessage.getHeaders().get(AmqpHeaders.APP_ID)).isNull();
		assertThat(siMessage.getHeaders().get(AmqpHeaders.CONTENT_TYPE)).isNull();
	}

	@Test
	public void withHeaderMapperDefaultMapping() throws Exception {
		AmqpInboundChannelAdapter adapter = context.getBean("withHeaderMapperDefaultMapping",
				AmqpInboundChannelAdapter.class);

		AbstractMessageListenerContainer mlc =
				TestUtils.getPropertyValue(adapter, "messageListenerContainer", AbstractMessageListenerContainer.class);
		ChannelAwareMessageListener listener = TestUtils.getPropertyValue(mlc, "messageListener",
				ChannelAwareMessageListener.class);
		MessageProperties amqpProperties = new MessageProperties();
		amqpProperties.setAppId("test.appId");
		amqpProperties.setClusterId("test.clusterId");
		amqpProperties.setContentEncoding("test.contentEncoding");
		amqpProperties.setContentLength(99L);
		amqpProperties.setContentType("test.contentType");
		amqpProperties.setHeader("foo", "foo");
		amqpProperties.setHeader("bar", "bar");
		Message amqpMessage = new Message("hello".getBytes(), amqpProperties);
		listener.onMessage(amqpMessage, null);
		QueueChannel requestChannel = context.getBean("requestChannel", QueueChannel.class);
		org.springframework.messaging.Message<?> siMessage = requestChannel.receive(0);
		assertThat(siMessage.getHeaders().get("bar")).isNotNull();
		assertThat(siMessage.getHeaders().get("foo")).isNotNull();
		assertThat(siMessage.getHeaders().get(AmqpHeaders.CONTENT_ENCODING)).isNotNull();
		assertThat(siMessage.getHeaders().get(AmqpHeaders.CLUSTER_ID)).isNotNull();
		assertThat(siMessage.getHeaders().get(AmqpHeaders.APP_ID)).isNotNull();
		assertThat(siMessage.getHeaders().get(AmqpHeaders.CONTENT_TYPE)).isNotNull();
	}

	@Test
	public void testInt2971HeaderMapperAndMappedHeadersExclusivity() {
		assertThatExceptionOfType(BeanDefinitionParsingException.class)
				.isThrownBy(() ->
						new ClassPathXmlApplicationContext(
								"AmqpInboundChannelAdapterParserTests-headerMapper-fail-context.xml",
								getClass()))
				.withMessageStartingWith("Configuration problem: The 'header-mapper' attribute " +
						"is mutually exclusive with 'mapped-request-headers' or 'mapped-reply-headers'");
	}

}
