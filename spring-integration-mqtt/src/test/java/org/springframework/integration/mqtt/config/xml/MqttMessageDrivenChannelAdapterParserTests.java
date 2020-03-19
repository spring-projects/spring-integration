/*
 * Copyright 2002-2020 the original author or authors.
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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collection;
import java.util.Iterator;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.support.MqttMessageConverter;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.MessageChannel;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @since 4.0
 *
 */
@RunWith(SpringRunner.class)
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
	public void testNoTopics() { // INT-3467 no longer required to have topics
		assertThat(TestUtils.getPropertyValue(noTopicsAdapter, "url")).isEqualTo("tcp://localhost:1883");
		assertThat(TestUtils.getPropertyValue(noTopicsAdapter, "autoStartup", Boolean.class)).isFalse();
		assertThat(TestUtils.getPropertyValue(noTopicsAdapter, "clientId")).isEqualTo("foo");
		assertThat(TestUtils.getPropertyValue(noTopicsAdapter, "topics", Collection.class).size()).isEqualTo(0);
		assertThat(TestUtils.getPropertyValue(noTopicsAdapter, "outputChannel")).isSameAs(out);
		assertThat(TestUtils.getPropertyValue(noTopicsAdapter, "clientFactory")).isSameAs(clientFactory);
		assertThat(TestUtils.getPropertyValue(this.noTopicsAdapter, "recoveryInterval")).isEqualTo(5000);
		assertThat(TestUtils.getPropertyValue(this.noTopicsAdapter, "manualAcks", Boolean.class)).isTrue();
	}

	@Test
	public void testNoTopicsDefaultCF() { // INT-3598
		assertThat(TestUtils.getPropertyValue(noTopicsAdapterDefaultCF, "url")).isEqualTo("tcp://localhost:1883");
		assertThat(TestUtils.getPropertyValue(noTopicsAdapterDefaultCF, "autoStartup", Boolean.class)).isFalse();
		assertThat(TestUtils.getPropertyValue(noTopicsAdapterDefaultCF, "clientId")).isEqualTo("foo");
		assertThat(TestUtils.getPropertyValue(noTopicsAdapterDefaultCF, "topics", Collection.class).size())
				.isEqualTo(0);
		assertThat(TestUtils.getPropertyValue(noTopicsAdapterDefaultCF, "outputChannel")).isSameAs(out);
		assertThat(TestUtils.getPropertyValue(this.noTopicsAdapterDefaultCF, "manualAcks", Boolean.class)).isFalse();
	}

	@Test
	public void testOneTopic() {
		assertThat(TestUtils.getPropertyValue(oneTopicAdapter, "url")).isEqualTo("tcp://localhost:1883");
		assertThat(TestUtils.getPropertyValue(oneTopicAdapter, "autoStartup", Boolean.class)).isFalse();
		assertThat(TestUtils.getPropertyValue(oneTopicAdapter, "phase")).isEqualTo(25);
		assertThat(TestUtils.getPropertyValue(oneTopicAdapter, "clientId")).isEqualTo("foo");
		assertThat(TestUtils.getPropertyValue(oneTopicAdapter, "topics", Collection.class).iterator().next().toString())
				.isEqualTo("Topic [topic=bar, qos=1]");
		assertThat(TestUtils.getPropertyValue(oneTopicAdapter, "converter")).isSameAs(converter);
		assertThat(TestUtils.getPropertyValue(oneTopicAdapter, "messagingTemplate.sendTimeout")).isEqualTo(123L);
		assertThat(TestUtils.getPropertyValue(oneTopicAdapter, "outputChannel")).isSameAs(out);
		assertThat(TestUtils.getPropertyValue(oneTopicAdapter, "clientFactory")).isSameAs(clientFactory);
		assertThat(TestUtils.getPropertyValue(oneTopicAdapter, "errorChannel")).isSameAs(errors);
	}

	@Test
	public void testTwoTopics() {
		assertThat(TestUtils.getPropertyValue(twoTopicsAdapter, "url")).isEqualTo("tcp://localhost:1883");
		assertThat(TestUtils.getPropertyValue(twoTopicsAdapter, "autoStartup", Boolean.class)).isFalse();
		assertThat(TestUtils.getPropertyValue(twoTopicsAdapter, "phase")).isEqualTo(25);
		assertThat(TestUtils.getPropertyValue(twoTopicsAdapter, "clientId")).isEqualTo("foo");
		Iterator<?> iterator = TestUtils.getPropertyValue(twoTopicsAdapter, "topics", Collection.class).iterator();
		assertThat(iterator.next().toString()).isEqualTo("Topic [topic=bar, qos=0]");
		assertThat(iterator.next().toString()).isEqualTo("Topic [topic=baz, qos=2]");
		assertThat(TestUtils.getPropertyValue(twoTopicsAdapter, "converter")).isSameAs(converter);
		assertThat(TestUtils.getPropertyValue(twoTopicsAdapter, "messagingTemplate.sendTimeout")).isEqualTo(123L);
		assertThat(TestUtils.getPropertyValue(twoTopicsAdapter, "outputChannel")).isSameAs(out);
		assertThat(TestUtils.getPropertyValue(twoTopicsAdapter, "clientFactory")).isSameAs(clientFactory);
	}

	@Test
	public void testTwoTopicsSingleQos() {
		Iterator<?> iterator = TestUtils.getPropertyValue(twoTopicsSingleQosAdapter, "topics", Collection.class).iterator();
		assertThat(iterator.next().toString()).isEqualTo("Topic [topic=bar, qos=0]");
		assertThat(iterator.next().toString()).isEqualTo("Topic [topic=baz, qos=0]");
	}

}
