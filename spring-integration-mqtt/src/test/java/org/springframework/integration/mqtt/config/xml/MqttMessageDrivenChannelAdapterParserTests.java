/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.integration.mqtt.config.xml;

import java.util.Map;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.support.MqttMessageConverter;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.0
 *
 */
@SpringJUnitConfig
@DirtiesContext
public class MqttMessageDrivenChannelAdapterParserTests {

	@Autowired
	private MqttPahoMessageDrivenChannelAdapter noTopicsAdapter;

	@Autowired
	private MqttPahoMessageDrivenChannelAdapter noTopicsAdapterDefaultCF;

	@Autowired
	private MqttPahoMessageDrivenChannelAdapter oneTopicAdapter;

	@Autowired
	private MqttPahoMessageDrivenChannelAdapter twoTopicsAdapter;

	@Autowired
	private MqttPahoMessageDrivenChannelAdapter twoTopicsSingleQosAdapter;

	@Autowired
	private MessageChannel out;

	@Autowired
	private MqttMessageConverter converter;

	@Autowired
	private DefaultMqttPahoClientFactory clientFactory;

	@Autowired
	private MessageChannel errors;

	@Test
	@SuppressWarnings("unchecked")
	public void testNoTopics() {
		assertThat(TestUtils.getPropertyValue(noTopicsAdapter, "url")).isEqualTo("tcp://localhost:1883");
		assertThat(TestUtils.getPropertyValue(noTopicsAdapter, "autoStartup", Boolean.class)).isFalse();
		assertThat(TestUtils.getPropertyValue(noTopicsAdapter, "clientId")).isEqualTo("foo");
		assertThat(TestUtils.getPropertyValue(noTopicsAdapter, "topics", Map.class)).hasSize(0);
		assertThat(TestUtils.getPropertyValue(noTopicsAdapter, "outputChannel")).isSameAs(out);
		assertThat(TestUtils.getPropertyValue(noTopicsAdapter, "clientFactory")).isSameAs(clientFactory);
		assertThat(TestUtils.getPropertyValue(this.noTopicsAdapter, "manualAcks", Boolean.class)).isTrue();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testNoTopicsDefaultCF() {
		assertThat(TestUtils.getPropertyValue(noTopicsAdapterDefaultCF, "url")).isEqualTo("tcp://localhost:1883");
		assertThat(TestUtils.getPropertyValue(noTopicsAdapterDefaultCF, "autoStartup", Boolean.class)).isFalse();
		assertThat(TestUtils.getPropertyValue(noTopicsAdapterDefaultCF, "clientId")).isEqualTo("foo");
		assertThat(TestUtils.getPropertyValue(noTopicsAdapterDefaultCF, "topics", Map.class)).hasSize(0);
		assertThat(TestUtils.getPropertyValue(noTopicsAdapterDefaultCF, "outputChannel")).isSameAs(out);
		assertThat(TestUtils.getPropertyValue(this.noTopicsAdapterDefaultCF, "manualAcks", Boolean.class)).isFalse();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testOneTopic() {
		assertThat(TestUtils.getPropertyValue(oneTopicAdapter, "url")).isEqualTo("tcp://localhost:1883");
		assertThat(TestUtils.getPropertyValue(oneTopicAdapter, "autoStartup", Boolean.class)).isFalse();
		assertThat(TestUtils.getPropertyValue(oneTopicAdapter, "phase")).isEqualTo(25);
		assertThat(TestUtils.getPropertyValue(oneTopicAdapter, "clientId")).isEqualTo("foo");
		assertThat(TestUtils.getPropertyValue(oneTopicAdapter, "topics", Map.class)).containsEntry("bar", 1);
		assertThat(TestUtils.getPropertyValue(oneTopicAdapter, "converter")).isSameAs(converter);
		assertThat(TestUtils.getPropertyValue(oneTopicAdapter, "messagingTemplate.sendTimeout")).isEqualTo(123L);
		assertThat(TestUtils.getPropertyValue(oneTopicAdapter, "outputChannel")).isSameAs(out);
		assertThat(TestUtils.getPropertyValue(oneTopicAdapter, "clientFactory")).isSameAs(clientFactory);
		assertThat(TestUtils.getPropertyValue(oneTopicAdapter, "errorChannel")).isSameAs(errors);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testTwoTopics() {
		assertThat(TestUtils.getPropertyValue(twoTopicsAdapter, "url")).isEqualTo("tcp://localhost:1883");
		assertThat(TestUtils.getPropertyValue(twoTopicsAdapter, "autoStartup", Boolean.class)).isFalse();
		assertThat(TestUtils.getPropertyValue(twoTopicsAdapter, "phase")).isEqualTo(25);
		assertThat(TestUtils.getPropertyValue(twoTopicsAdapter, "clientId")).isEqualTo("foo");
		assertThat(TestUtils.getPropertyValue(twoTopicsAdapter, "topics", Map.class))
				.containsEntry("bar", 0)
				.containsEntry("baz", 2);
		assertThat(TestUtils.getPropertyValue(twoTopicsAdapter, "converter")).isSameAs(converter);
		assertThat(TestUtils.getPropertyValue(twoTopicsAdapter, "messagingTemplate.sendTimeout")).isEqualTo(123L);
		assertThat(TestUtils.getPropertyValue(twoTopicsAdapter, "outputChannel")).isSameAs(out);
		assertThat(TestUtils.getPropertyValue(twoTopicsAdapter, "clientFactory")).isSameAs(clientFactory);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testTwoTopicsSingleQos() {
		assertThat(TestUtils.getPropertyValue(twoTopicsSingleQosAdapter, "topics", Map.class))
				.containsEntry("bar", 0)
				.containsEntry("baz", 0);
	}

}
