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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.event.MqttMessageDeliveredEvent;
import org.springframework.integration.mqtt.event.MqttMessageSentEvent;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
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
public class BackToBackAdapterTests {

	@ClassRule
	public static final BrokerRunning brokerRunning = BrokerRunning.isRunning(1883);

	@Autowired
	private MessageChannel out;

	@Autowired
	private PollableChannel in;

	@Test
	public void testSingleTopic() {
		MqttPahoMessageHandler adapter = new MqttPahoMessageHandler("tcp://localhost:1883", "si-test-out");
		adapter.setDefaultTopic("mqtt-foo");
		adapter.setBeanFactory(mock(BeanFactory.class));
		adapter.afterPropertiesSet();
		adapter.start();
		MqttPahoMessageDrivenChannelAdapter inbound = new MqttPahoMessageDrivenChannelAdapter("tcp://localhost:1883",
				"si-test-in", "mqtt-foo");
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
	public void testAddRemoveTopic() {
		MqttPahoMessageHandler adapter = new MqttPahoMessageHandler("tcp://localhost:1883", "si-test-out");
		adapter.setDefaultTopic("mqtt-foo");
		adapter.setBeanFactory(mock(BeanFactory.class));
		adapter.afterPropertiesSet();
		adapter.start();
		MqttPahoMessageDrivenChannelAdapter inbound = new MqttPahoMessageDrivenChannelAdapter("tcp://localhost:1883", "si-test-in");
		QueueChannel outputChannel = new QueueChannel();
		inbound.setOutputChannel(outputChannel);
		ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.initialize();
		inbound.setTaskScheduler(taskScheduler);
		inbound.setBeanFactory(mock(BeanFactory.class));
		inbound.afterPropertiesSet();
		inbound.start();
		inbound.addTopic("mqtt-foo");
		adapter.handleMessage(new GenericMessage<String>("foo"));
		Message<?> out = outputChannel.receive(10000);
		assertNotNull(out);
		assertEquals("foo", out.getPayload());
		assertEquals("mqtt-foo", out.getHeaders().get(MqttHeaders.TOPIC));

		inbound.addTopic("mqtt-bar");
		adapter.handleMessage(MessageBuilder.withPayload("bar").setHeader(MqttHeaders.TOPIC, "mqtt-bar").build());
		out = outputChannel.receive(10000);
		assertNotNull(out);
		assertEquals("bar", out.getPayload());
		assertEquals("mqtt-bar", out.getHeaders().get(MqttHeaders.TOPIC));

		inbound.removeTopic("mqtt-bar");
		adapter.handleMessage(MessageBuilder.withPayload("bar").setHeader(MqttHeaders.TOPIC, "mqtt-bar").build());
		out = outputChannel.receive(1000);
		assertNull(out);

		try {
			inbound.addTopic("mqtt-foo");
			fail("Expected exception");
		}
		catch (MessagingException e) {
			assertEquals("Topic 'mqtt-foo' is already subscribed.", e.getMessage());
		}

		inbound.addTopic("mqqt-bar", "mqqt-baz");
		inbound.removeTopic("mqqt-bar", "mqqt-baz");
		inbound.addTopics(new String[] { "mqqt-bar", "mqqt-baz" }, new int[] { 0, 0 });
		inbound.removeTopic("mqqt-bar", "mqqt-baz");

		adapter.stop();
		inbound.stop();
	}

	@Test
	public void testTwoTopics() {
		MqttPahoMessageHandler adapter = new MqttPahoMessageHandler("tcp://localhost:1883", "si-test-out");
		adapter.setDefaultTopic("mqtt-foo");
		adapter.setBeanFactory(mock(BeanFactory.class));
		adapter.afterPropertiesSet();
		adapter.start();
		MqttPahoMessageDrivenChannelAdapter inbound = new MqttPahoMessageDrivenChannelAdapter("tcp://localhost:1883",
				"si-test-in", "mqtt-foo", "mqtt-bar");
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
		adapter.setAsyncEvents(true);
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
		verifyEvents(adapter, publisher, message);
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
		adapter.setAsyncEvents(true);
		adapter.setDefaultQos(1);
		EventPublisher publisher1 = new EventPublisher();
		adapter.setApplicationEventPublisher(publisher1);
		adapter.afterPropertiesSet();
		adapter.start();

		MqttPahoMessageDrivenChannelAdapter inbound =
				new MqttPahoMessageDrivenChannelAdapter("tcp://localhost:1883", "si-test-in", "mqtt-foo", "mqtt-bar");
		QueueChannel outputChannel = new QueueChannel();
		inbound.setOutputChannel(outputChannel);
		ThreadPoolTaskScheduler taskScheduler = new ThreadPoolTaskScheduler();
		taskScheduler.initialize();
		inbound.setTaskScheduler(taskScheduler);
		inbound.setBeanFactory(mock(BeanFactory.class));
		inbound.afterPropertiesSet();
		inbound.start();
		Message<String> message1 = new GenericMessage<String>("foo");
		adapter.handleMessage(message1);
		verifyEvents(adapter, publisher1, message1);

		Message<String> message2 = MessageBuilder.withPayload("bar")
				.setHeader(MqttHeaders.TOPIC, "mqtt-bar")
				.build();
		EventPublisher publisher2 = new EventPublisher();
		adapter.setApplicationEventPublisher(publisher2);
		adapter.handleMessage(message2);
		verifyEvents(adapter, publisher2, message2);

		verifyMessageIds(publisher1, publisher2);
		int clientInstance = publisher1.delivered.getClientInstance();

		adapter.stop();
		adapter.start(); // new client instance

		publisher1 = new EventPublisher();
		adapter.setApplicationEventPublisher(publisher1);
		adapter.handleMessage(message1);
		verifyEvents(adapter, publisher1, message1);

		publisher2 = new EventPublisher();
		adapter.setApplicationEventPublisher(publisher2);
		adapter.handleMessage(message2);
		verifyEvents(adapter, publisher2, message2);

		verifyMessageIds(publisher1, publisher2);

		assertNotEquals(clientInstance, publisher1.delivered.getClientInstance());
		adapter.stop();

		Message<?> out = null;
		for (int i = 0; i < 4; i++) {
			out = outputChannel.receive(10000);
			assertNotNull(out);
			if ("foo".equals(out.getPayload())) {
				assertEquals("mqtt-foo", out.getHeaders().get(MqttHeaders.TOPIC));
			}
			else if ("bar".equals(out.getPayload())) {
				assertEquals("mqtt-bar", out.getHeaders().get(MqttHeaders.TOPIC));
			}
			else {
				fail("unexpected payload " + out.getPayload());
			}
		}
		inbound.stop();
	}

	private void verifyEvents(MqttPahoMessageHandler adapter, EventPublisher publisher1, Message<String> message1)
			throws InterruptedException {
		assertTrue(publisher1.latch.await(10, TimeUnit.SECONDS));
		assertNotNull(publisher1.sent);
		assertNotNull(publisher1.delivered);
		assertEquals(publisher1.sent.getMessageId(), publisher1.delivered.getMessageId());
		assertEquals(adapter.getClientId(), publisher1.sent.getClientId());
		assertEquals(adapter.getClientId(), publisher1.delivered.getClientId());
		assertEquals(adapter.getClientInstance(), publisher1.sent.getClientInstance());
		assertEquals(adapter.getClientInstance(), publisher1.delivered.getClientInstance());
		assertSame(message1, publisher1.sent.getMessage());
	}

	private void verifyMessageIds(EventPublisher publisher1, EventPublisher publisher2) {
		assertNotEquals(publisher1.delivered.getMessageId(), publisher2.delivered.getMessageId());
		assertEquals(publisher1.delivered.getClientId(), publisher2.delivered.getClientId());
		assertEquals(publisher1.delivered.getClientInstance(), publisher2.delivered.getClientInstance());
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
