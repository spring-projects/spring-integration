/*
 * Copyright 2002-2014 the original author or authors.
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
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import java.io.File;

import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * @author Gary Russell
 * @since 4.0
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext
public class BackTobackAdapterTests {

	@Rule
	public final BrokerRunning brokerRunning = BrokerRunning.isRunning(1883);

	@Autowired
	public MessageChannel out;

	@Autowired
	public PollableChannel in;

	@Test
	public void testSingleTopic() {
		MqttPahoMessageHandler adapter = new MqttPahoMessageHandler("tcp://localhost:1883", "si-test-out");
		adapter.setDefaultTopic("mqtt-foo");
		adapter.setBeanFactory(mock(BeanFactory.class));
		adapter.afterPropertiesSet();
		adapter.start();
		MqttPahoMessageDrivenChannelAdapter inbound = new MqttPahoMessageDrivenChannelAdapter("tcp://localhost:1883", "si-test-in", "mqtt-foo");
		QueueChannel outputChannel = new QueueChannel();
		inbound.setOutputChannel(outputChannel);
		ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.initialize();
		inbound.setTaskScheduler(taskScheduler);
		inbound.setBeanFactory(mock(BeanFactory.class));
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
		adapter.setBeanFactory(mock(BeanFactory.class));
		adapter.afterPropertiesSet();
		adapter.start();
		MqttPahoMessageDrivenChannelAdapter inbound = new MqttPahoMessageDrivenChannelAdapter("tcp://localhost:1883", "si-test-in", "mqtt-foo", "mqtt-bar");
		QueueChannel outputChannel = new QueueChannel();
		inbound.setOutputChannel(outputChannel);
		ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.initialize();
		inbound.setTaskScheduler(taskScheduler);
		inbound.setBeanFactory(mock(BeanFactory.class));
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
		assertEquals("mqtt-bar", out.getHeaders().get(MqttHeaders.TOPIC));
	}

	@Test
	public void testAsync() {
		MqttPahoMessageHandler adapter = new MqttPahoMessageHandler("tcp://localhost:1883", "si-test-out");
		adapter.setDefaultTopic("mqtt-foo");
		adapter.setBeanFactory(mock(BeanFactory.class));
		adapter.setAsync(true);
		QueueChannel deliveryCompleteChannel = new QueueChannel();
		adapter.setDeliveryCompleteChannel(deliveryCompleteChannel);
		adapter.afterPropertiesSet();
		adapter.start();
		MqttPahoMessageDrivenChannelAdapter inbound =
				new MqttPahoMessageDrivenChannelAdapter("tcp://localhost:1883", "si-test-in", "mqtt-foo");
		QueueChannel outputChannel = new QueueChannel();
		inbound.setOutputChannel(outputChannel);
		ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.initialize();
		inbound.setTaskScheduler(taskScheduler);
		inbound.setBeanFactory(mock(BeanFactory.class));
		inbound.afterPropertiesSet();
		inbound.start();
		adapter.handleMessage(new GenericMessage<String>("foo"));
		Message<?> delivery1 = deliveryCompleteChannel.receive(10000);
		assertNotNull(delivery1);
		Message<?> delivery2 = deliveryCompleteChannel.receive(10000);
		assertNotNull(delivery2);
		if (delivery1.getPayload().equals("foo")) {
			assertEquals(delivery1.getHeaders().get(MqttHeaders.MESSAGE_ID), delivery2.getPayload());
		}
		else if (delivery2.getPayload().equals("foo")) {
			assertEquals(delivery2.getHeaders().get(MqttHeaders.MESSAGE_ID), delivery1.getPayload());
		}
		else {
			fail("Unexpected delivery messages " + delivery1 + " " + delivery2);
		}
		assertEquals("mqtt-foo", delivery1.getHeaders().get(MqttHeaders.TOPIC));
		assertEquals("mqtt-foo", delivery2.getHeaders().get(MqttHeaders.TOPIC));
		adapter.stop();
		Message<?> out = outputChannel.receive(10000);
		assertNotNull(out);
		inbound.stop();
		assertEquals("foo", out.getPayload());
		assertEquals("mqtt-foo", out.getHeaders().get(MqttHeaders.TOPIC));
	}

	@Test
	public void testAsyncPersisted() {
		DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
		String tmpDir = System.getProperty("java.io.tmpdir") + File.separator + "mqtt_persist";
		new File(tmpDir).mkdirs();
		MqttClientPersistence persistence = new MqttDefaultFilePersistence(tmpDir);
		factory.setPersistence(persistence);
		MqttPahoMessageHandler adapter = new MqttPahoMessageHandler("tcp://localhost:1883", "si-test-out", factory);
		adapter.setDefaultTopic("mqtt-foo");
		adapter.setBeanFactory(mock(BeanFactory.class));
		adapter.setAsync(true);
		QueueChannel deliveryCompleteChannel = new QueueChannel();
		adapter.setDeliveryCompleteChannel(deliveryCompleteChannel);
		adapter.setDefaultQos(1);
		adapter.afterPropertiesSet();
		adapter.start();

		MqttPahoMessageDrivenChannelAdapter inbound =
				new MqttPahoMessageDrivenChannelAdapter("tcp://localhost:1883", "si-test-in", "mqtt-foo");
		QueueChannel outputChannel = new QueueChannel();
		inbound.setOutputChannel(outputChannel);
		ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.initialize();
		inbound.setTaskScheduler(taskScheduler);
		inbound.setBeanFactory(mock(BeanFactory.class));
		inbound.afterPropertiesSet();
		inbound.start();
		adapter.handleMessage(new GenericMessage<String>("foo"));
		Message<?> delivery1 = deliveryCompleteChannel.receive(10000);
		assertNotNull(delivery1);
		Message<?> delivery2 = deliveryCompleteChannel.receive(10000);
		assertNotNull(delivery2);
		if (delivery1.getPayload().equals("foo")) {
			assertEquals(delivery1.getHeaders().get(MqttHeaders.MESSAGE_ID), delivery2.getPayload());
		}
		else if (delivery2.getPayload().equals("foo")) {
			assertEquals(delivery2.getHeaders().get(MqttHeaders.MESSAGE_ID), delivery1.getPayload());
		}
		else {
			fail("Unexpected delivery messages " + delivery1 + " " + delivery2);
		}
		assertEquals("mqtt-foo", delivery1.getHeaders().get(MqttHeaders.TOPIC));
		assertEquals("mqtt-foo", delivery2.getHeaders().get(MqttHeaders.TOPIC));
		adapter.stop();
		Message<?> out = outputChannel.receive(10000);
		assertNotNull(out);
		inbound.stop();
		assertEquals("foo", out.getPayload());
		assertEquals("mqtt-foo", out.getHeaders().get(MqttHeaders.TOPIC));
	}

	@Test
	public void testMultiURIs() {
		out.send(new GenericMessage<String>("foo"));
		Message<?> message = in.receive(10000);
		assertNotNull(message);
		assertEquals("foo", message.getPayload());
	}
}
