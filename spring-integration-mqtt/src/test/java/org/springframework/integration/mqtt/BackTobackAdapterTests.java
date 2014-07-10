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
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttMessageDeliveredEvent;
import org.springframework.integration.mqtt.outbound.MqttMessageSentEvent;
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
	public void testAsync() throws Exception {
		MqttPahoMessageHandler adapter = new MqttPahoMessageHandler("tcp://localhost:1883", "si-test-out");
		adapter.setDefaultTopic("mqtt-foo");
		adapter.setBeanFactory(mock(BeanFactory.class));
		adapter.setAsync(true);
		EventPublisher publisher = new EventPublisher();
		adapter.setApplicationEventPublisher(publisher);
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
		GenericMessage<String> message = new GenericMessage<String>("foo");
		adapter.handleMessage(message);
		assertTrue(publisher.latch.await(10, TimeUnit.SECONDS));
		assertNotNull(publisher.sent);
		assertNotNull(publisher.delivered);
		assertEquals(publisher.sent.getMessageId(), publisher.delivered.getMessageId());
		assertSame(message, publisher.sent.getMessage());
		adapter.stop();
		Message<?> out = outputChannel.receive(10000);
		assertNotNull(out);
		inbound.stop();
		assertEquals("foo", out.getPayload());
		assertEquals("mqtt-foo", out.getHeaders().get(MqttHeaders.TOPIC));
	}

	@Test
	public void testAsyncPersisted() throws Exception {
		DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
		String tmpDir = System.getProperty("java.io.tmpdir") + File.separator + "mqtt_persist";
		new File(tmpDir).mkdirs();
		MqttClientPersistence persistence = new MqttDefaultFilePersistence(tmpDir);
		factory.setPersistence(persistence);
		MqttPahoMessageHandler adapter = new MqttPahoMessageHandler("tcp://localhost:1883", "si-test-out", factory);
		adapter.setDefaultTopic("mqtt-foo");
		adapter.setBeanFactory(mock(BeanFactory.class));
		adapter.setAsync(true);
		adapter.setDefaultQos(1);
		EventPublisher publisher = new EventPublisher();
		adapter.setApplicationEventPublisher(publisher);
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
		GenericMessage<String> message = new GenericMessage<String>("foo");
		adapter.handleMessage(message);
		assertTrue(publisher.latch.await(10, TimeUnit.SECONDS));
		assertNotNull(publisher.sent);
		assertNotNull(publisher.delivered);
		assertEquals(publisher.sent.getMessageId(), publisher.delivered.getMessageId());
		assertSame(message, publisher.sent.getMessage());
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

	private class EventPublisher implements ApplicationEventPublisher {

		private volatile MqttMessageDeliveredEvent delivered;

		private MqttMessageSentEvent sent;

		private final CountDownLatch latch = new CountDownLatch(2);

		@Override
		public void publishEvent(ApplicationEvent event) {
			if (event instanceof MqttMessageSentEvent) {
				this.sent = (MqttMessageSentEvent) event;
			}
			else if (event instanceof MqttMessageDeliveredEvent){
				this.delivered = (MqttMessageDeliveredEvent) event;
			}
			latch.countDown();
		}

	}

}
