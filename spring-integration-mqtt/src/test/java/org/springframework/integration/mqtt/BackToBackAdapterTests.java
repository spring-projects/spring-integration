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

package org.springframework.integration.mqtt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;

import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.ApplicationListener;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoComponent;
import org.springframework.integration.mqtt.event.MqttMessageDeliveredEvent;
import org.springframework.integration.mqtt.event.MqttMessageSentEvent;
import org.springframework.integration.mqtt.event.MqttSubscribedEvent;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.support.json.EmbeddedJsonHeadersMessageMapper;
import org.springframework.integration.support.json.JacksonJsonUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.0
 *
 */
@RunWith(SpringRunner.class)
@DirtiesContext
public class BackToBackAdapterTests {

	@ClassRule
	public static final BrokerRunning brokerRunning = BrokerRunning.isRunning(1883);

	@ClassRule
	public static final TemporaryFolder folder = new TemporaryFolder();

	@Autowired
	private MessageChannel out;

	@Autowired
	private PollableChannel in;

	@Autowired
	private EventsListener listener;

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
		Message<?> out = outputChannel.receive(20000);
		assertThat(out).isNotNull();
		adapter.stop();
		inbound.stop();
		assertThat(out.getPayload()).isEqualTo("foo");
		assertThat(out.getHeaders().get(MqttHeaders.RECEIVED_TOPIC)).isEqualTo("mqtt-foo");
		assertThat(adapter.getConnectionInfo().getServerURIs()[0]).isEqualTo("tcp://localhost:1883");
	}

	@Test
	public void testJson() {
		testJsonCommon("org.springframework");
	}

	@Test
	public void testJsonNoTrust() {
		testJsonCommon();
	}

	private void testJsonCommon(String... trusted) {
		MqttPahoMessageHandler adapter = new MqttPahoMessageHandler("tcp://localhost:1883", "si-test-out");
		adapter.setDefaultTopic("mqtt-foo");
		adapter.setBeanFactory(mock(BeanFactory.class));
		EmbeddedJsonHeadersMessageMapper mapper = new EmbeddedJsonHeadersMessageMapper(
				JacksonJsonUtils.messagingAwareMapper("org.springframework"));
		DefaultPahoMessageConverter converter = new DefaultPahoMessageConverter();
		converter.setBytesMessageMapper(mapper);
		adapter.setConverter(converter);
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
		inbound.setConverter(converter);
		inbound.afterPropertiesSet();
		inbound.start();
		adapter.handleMessage(new GenericMessage<Foo>(new Foo("bar"), Collections.singletonMap("baz", "qux")));
		Message<?> out = outputChannel.receive(20000);
		assertThat(out).isNotNull();
		adapter.stop();
		inbound.stop();
		if (trusted != null) {
			assertThat(out.getPayload()).isEqualTo(new Foo("bar"));
		}
		else {
			assertThat(out.getPayload()).isNotEqualTo(new Foo("bar"));
		}
		assertThat(out.getHeaders().get(MqttHeaders.RECEIVED_TOPIC)).isEqualTo("mqtt-foo");
		assertThat(out.getHeaders().get("baz")).isEqualTo("qux");
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
		Message<?> out = outputChannel.receive(20_000);
		assertThat(out).isNotNull();
		assertThat(out.getPayload()).isEqualTo("foo");
		assertThat(out.getHeaders().get(MqttHeaders.RECEIVED_TOPIC)).isEqualTo("mqtt-foo");

		inbound.addTopic("mqtt-bar");
		adapter.handleMessage(MessageBuilder.withPayload("bar").setHeader(MqttHeaders.TOPIC, "mqtt-bar").build());
		out = outputChannel.receive(20_000);
		assertThat(out).isNotNull();
		assertThat(out.getPayload()).isEqualTo("bar");
		assertThat(out.getHeaders().get(MqttHeaders.RECEIVED_TOPIC)).isEqualTo("mqtt-bar");

		inbound.removeTopic("mqtt-bar");
		adapter.handleMessage(MessageBuilder.withPayload("bar").setHeader(MqttHeaders.TOPIC, "mqtt-bar").build());
		out = outputChannel.receive(1);
		assertThat(out).isNull();

		try {
			inbound.addTopic("mqtt-foo");
			fail("Expected exception");
		}
		catch (MessagingException e) {
			assertThat(e.getMessage()).isEqualTo("Topic 'mqtt-foo' is already subscribed.");
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
		Message<?> out = outputChannel.receive(20000);
		assertThat(out).isNotNull();
		assertThat(out.getPayload()).isEqualTo("foo");
		assertThat(out.getHeaders().get(MqttHeaders.RECEIVED_TOPIC)).isEqualTo("mqtt-foo");
		out = outputChannel.receive(20000);
		assertThat(out).isNotNull();
		inbound.stop();
		assertThat(out.getPayload()).isEqualTo("bar");
		assertThat(out.getHeaders().get(MqttHeaders.RECEIVED_TOPIC)).isEqualTo("mqtt-bar");
		adapter.stop();
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
		Message<?> out = outputChannel.receive(20000);
		assertThat(out).isNotNull();
		adapter.stop();
		inbound.stop();
		assertThat(out.getPayload()).isEqualTo("foo");
		assertThat(out.getHeaders().get(MqttHeaders.RECEIVED_TOPIC)).isEqualTo("mqtt-foo");
	}

	@Test
	public void testAsyncPersisted() throws Exception {
		DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
		MqttClientPersistence persistence = new MqttDefaultFilePersistence(folder.getRoot().getAbsolutePath());
		factory.setPersistence(persistence);
		MqttPahoMessageHandler adapter = new MqttPahoMessageHandler("tcp://localhost:1883", "si-test-out",
				factory);
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

		assertThat(publisher1.delivered.getClientInstance()).isNotEqualTo(clientInstance);

		Message<?> out = null;
		for (int i = 0; i < 4; i++) {
			out = outputChannel.receive(20000);
			assertThat(out).isNotNull();
			if ("foo".equals(out.getPayload())) {
				assertThat(out.getHeaders().get(MqttHeaders.RECEIVED_TOPIC)).isEqualTo("mqtt-foo");
			}
			else if ("bar".equals(out.getPayload())) {
				assertThat(out.getHeaders().get(MqttHeaders.RECEIVED_TOPIC)).isEqualTo("mqtt-bar");
			}
			else {
				fail("unexpected payload " + out.getPayload());
			}
		}
		adapter.stop();
		inbound.stop();
	}

	private void verifyEvents(MqttPahoMessageHandler adapter, EventPublisher publisher1, Message<String> message1)
			throws InterruptedException {
		assertThat(publisher1.latch.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(publisher1.sent).isNotNull();
		assertThat(publisher1.delivered).isNotNull();
		assertThat(publisher1.delivered.getMessageId()).isEqualTo(publisher1.sent.getMessageId());
		assertThat(publisher1.sent.getClientId()).isEqualTo(adapter.getClientId());
		assertThat(publisher1.delivered.getClientId()).isEqualTo(adapter.getClientId());
		assertThat(publisher1.sent.getClientInstance()).isEqualTo(adapter.getClientInstance());
		assertThat(publisher1.delivered.getClientInstance()).isEqualTo(adapter.getClientInstance());
		assertThat(publisher1.sent.getMessage()).isSameAs(message1);
	}

	private void verifyMessageIds(EventPublisher publisher1, EventPublisher publisher2) {
		assertThat(publisher2.delivered.getMessageId()).isNotEqualTo(publisher1.delivered.getMessageId());
		assertThat(publisher2.delivered.getClientId()).isEqualTo(publisher1.delivered.getClientId());
		assertThat(publisher2.delivered.getClientInstance()).isEqualTo(publisher1.delivered.getClientInstance());
	}

	@Test
	public void testMultiURIs() {
		out.send(new GenericMessage<String>("foo"));
		Message<?> message = in.receive(20000);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isEqualTo("foo");
		MqttPahoComponent source = this.listener.event.getSourceAsType();
		assertThat(Arrays.toString(source.getConnectionInfo().getServerURIs()))
				.isEqualTo("[tcp://localhost:1883, tcp://localhost:1883]");
	}

	public static class EventsListener implements ApplicationListener<MqttSubscribedEvent> {

		volatile MqttSubscribedEvent event;

		@Override
		public void onApplicationEvent(MqttSubscribedEvent event) {
			this.event = event;
		}

	}

	private class EventPublisher implements ApplicationEventPublisher {

		private volatile MqttMessageDeliveredEvent delivered;

		private MqttMessageSentEvent sent;

		private final CountDownLatch latch = new CountDownLatch(2);

		EventPublisher() {
			super();
		}

		@Override
		public void publishEvent(ApplicationEvent event) {
			if (event instanceof MqttMessageSentEvent) {
				this.sent = (MqttMessageSentEvent) event;
			}
			else if (event instanceof MqttMessageDeliveredEvent) {
				this.delivered = (MqttMessageDeliveredEvent) event;
			}
			latch.countDown();
		}

		@Override
		public void publishEvent(Object event) {

		}

	}

	public static class Foo {

		private String bar;

		public Foo() {
			super();
		}

		public Foo(String bar) {
			this.bar = bar;
		}

		public String getBar() {
			return this.bar;
		}

		public void setBar(String bar) {
			this.bar = bar;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((this.bar == null) ? 0 : this.bar.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) {
				return true;
			}
			if (obj == null) {
				return false;
			}
			if (getClass() != obj.getClass()) {
				return false;
			}
			Foo other = (Foo) obj;
			if (this.bar == null) {
				if (other.bar != null) {
					return false;
				}
			}
			else if (!this.bar.equals(other.bar)) {
				return false;
			}
			return true;
		}

	}

}
