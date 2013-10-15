/*
 * Copyright 2002-2013 the original author or authors.
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
package org.springframework.integration.mqtt;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Rule;
import org.junit.Test;

import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.message.GenericMessage;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * @author Gary Russell
 * @since 1.0
 *
 */
public class BackTobackAdapterTests {

	@Rule
	public final BrokerRunning brokerRunning = BrokerRunning.isRunning(1883);

	@Test
	public void testSingleTopic() {
		MqttPahoMessageHandler adapter = new MqttPahoMessageHandler("tcp://localhost:1883", "si-test-out");
		adapter.setDefaultTopic("mqtt-foo");
		adapter.afterPropertiesSet();
		adapter.start();
		MqttPahoMessageDrivenChannelAdapter inbound = new MqttPahoMessageDrivenChannelAdapter("tcp://localhost:1883", "si-test-in", "mqtt-foo");
		QueueChannel outputChannel = new QueueChannel();
		inbound.setOutputChannel(outputChannel);
		ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.initialize();
		inbound.setTaskScheduler(taskScheduler);
		inbound.afterPropertiesSet();
		inbound.start();
		adapter.handleMessage(new GenericMessage<String>("foo"));
		adapter.stop();
		Message<?> out = outputChannel.receive(1000);
		assertNotNull(out);
		inbound.stop();
		assertEquals("foo", out.getPayload());
		assertEquals("mqtt-foo", out.getHeaders().get(MqttHeaders.TOPIC));
	}

	@Test
	public void testTwoTopics() {
		MqttPahoMessageHandler adapter = new MqttPahoMessageHandler("tcp://localhost:1883", "si-test-out");
		adapter.setDefaultTopic("mqtt-foo");
		adapter.afterPropertiesSet();
		adapter.start();
		MqttPahoMessageDrivenChannelAdapter inbound = new MqttPahoMessageDrivenChannelAdapter("tcp://localhost:1883", "si-test-in", "mqtt-foo", "mqtt-bar");
		QueueChannel outputChannel = new QueueChannel();
		inbound.setOutputChannel(outputChannel);
		ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.initialize();
		inbound.setTaskScheduler(taskScheduler);
		inbound.afterPropertiesSet();
		inbound.start();
		adapter.handleMessage(new GenericMessage<String>("foo"));
		Message<?> message = MessageBuilder.withPayload("bar").setHeader(MqttHeaders.TOPIC, "mqtt-bar").build();
		adapter.handleMessage(message);
		adapter.stop();
		Message<?> out = outputChannel.receive(1000);
		assertNotNull(out);
		inbound.stop();
		assertEquals("foo", out.getPayload());
		assertEquals("mqtt-foo", out.getHeaders().get(MqttHeaders.TOPIC));
		out = outputChannel.receive(1000);
		assertNotNull(out);
		inbound.stop();
		assertEquals("bar", out.getPayload());
		assertEquals("mqtt-bar", out.getHeaders().get(MqttHeaders.TOPIC));	}

}
