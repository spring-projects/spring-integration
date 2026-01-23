/*
 * Copyright 2002-present the original author or authors.
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
 * @author Glenn Renfro
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
		assertThat(TestUtils.<String>getPropertyValue(noTopicsAdapter, "url")).isEqualTo("tcp://localhost:1883");
		assertThat(TestUtils.<Boolean>getPropertyValue(noTopicsAdapter, "autoStartup")).isFalse();
		assertThat(TestUtils.<String>getPropertyValue(noTopicsAdapter, "clientId")).isEqualTo("foo");
		assertThat(TestUtils.<Map<?, ?>>getPropertyValue(noTopicsAdapter, "topics")).hasSize(0);
		assertThat(TestUtils.<Object>getPropertyValue(noTopicsAdapter, "outputChannel")).isSameAs(out);
		assertThat(TestUtils.<Object>getPropertyValue(noTopicsAdapter, "clientFactory")).isSameAs(clientFactory);
		assertThat(TestUtils.<Boolean>getPropertyValue(this.noTopicsAdapter, "manualAcks")).isTrue();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testNoTopicsDefaultCF() {
		assertThat(TestUtils.<String>getPropertyValue(noTopicsAdapterDefaultCF, "url"))
				.isEqualTo("tcp://localhost:1883");
		assertThat(TestUtils.<Boolean>getPropertyValue(noTopicsAdapterDefaultCF, "autoStartup")).isFalse();
		assertThat(TestUtils.<String>getPropertyValue(noTopicsAdapterDefaultCF, "clientId")).isEqualTo("foo");
		assertThat(TestUtils.<Map<?, ?>>getPropertyValue(noTopicsAdapterDefaultCF, "topics")).hasSize(0);
		assertThat(TestUtils.<Object>getPropertyValue(noTopicsAdapterDefaultCF, "outputChannel")).isSameAs(out);
		assertThat(TestUtils.<Boolean>getPropertyValue(this.noTopicsAdapterDefaultCF, "manualAcks")).isFalse();
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testOneTopic() {
		assertThat(TestUtils.<String>getPropertyValue(oneTopicAdapter, "url")).isEqualTo("tcp://localhost:1883");
		assertThat(TestUtils.<Boolean>getPropertyValue(oneTopicAdapter, "autoStartup")).isFalse();
		assertThat(TestUtils.<Integer>getPropertyValue(oneTopicAdapter, "phase")).isEqualTo(25);
		assertThat(TestUtils.<String>getPropertyValue(oneTopicAdapter, "clientId")).isEqualTo("foo");
		assertThat(TestUtils.<Map<String, Integer>>getPropertyValue(oneTopicAdapter, "topics")).containsEntry("bar", 1);
		assertThat(TestUtils.<MqttMessageConverter>getPropertyValue(oneTopicAdapter, "converter")).isSameAs(converter);
		assertThat(TestUtils.<Long>getPropertyValue(oneTopicAdapter, "messagingTemplate.sendTimeout")).
				isEqualTo(123L);
		assertThat(TestUtils.<Object>getPropertyValue(oneTopicAdapter, "outputChannel")).isSameAs(out);
		assertThat(TestUtils.<DefaultMqttPahoClientFactory>getPropertyValue(oneTopicAdapter, "clientFactory"))
				.isSameAs(clientFactory);
		assertThat(TestUtils.<MessageChannel>getPropertyValue(oneTopicAdapter, "errorChannel")).isSameAs(errors);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testTwoTopics() {
		assertThat(TestUtils.<String>getPropertyValue(twoTopicsAdapter, "url")).isEqualTo("tcp://localhost:1883");
		assertThat(TestUtils.<Boolean>getPropertyValue(twoTopicsAdapter, "autoStartup")).isFalse();
		assertThat(TestUtils.<Integer>getPropertyValue(twoTopicsAdapter, "phase")).isEqualTo(25);
		assertThat(TestUtils.<String>getPropertyValue(twoTopicsAdapter, "clientId")).isEqualTo("foo");
		assertThat(TestUtils.<Map<String, Integer>>getPropertyValue(twoTopicsAdapter, "topics"))
				.containsEntry("bar", 0)
				.containsEntry("baz", 2);
		assertThat(TestUtils.<Object>getPropertyValue(twoTopicsAdapter, "converter")).isSameAs(converter);
		assertThat(TestUtils.<Long>getPropertyValue(twoTopicsAdapter, "messagingTemplate.sendTimeout")).isEqualTo(123L);
		assertThat(TestUtils.<MessageChannel>getPropertyValue(twoTopicsAdapter, "outputChannel")).isSameAs(out);
		assertThat(TestUtils.<DefaultMqttPahoClientFactory>getPropertyValue(twoTopicsAdapter, "clientFactory"))
				.isSameAs(clientFactory);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void testTwoTopicsSingleQos() {
		assertThat(TestUtils.<Map<String, Integer>>getPropertyValue(twoTopicsSingleQosAdapter, "topics"))
				.containsEntry("bar", 0)
				.containsEntry("baz", 0);
	}

}
