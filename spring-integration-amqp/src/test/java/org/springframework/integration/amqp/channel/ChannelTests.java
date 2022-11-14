/*
 * Copyright 2014-2022 the original author or authors.
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

package org.springframework.integration.amqp.channel;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.junit.BrokerRunning;
import org.springframework.amqp.rabbit.listener.BlockingQueueConsumer;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.integration.amqp.config.AmqpChannelFactoryBean;
import org.springframework.integration.amqp.support.AmqpHeaderMapper;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.PollableChannel;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;

/**
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.0
 *
 */
@ContextConfiguration
@RunWith(SpringJUnit4ClassRunner.class)
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
public class ChannelTests {

	@ClassRule
	public static final BrokerRunning brokerIsRunning =
			BrokerRunning.isRunningWithEmptyQueues("pollableWithEP", "withEP", "testConvertFail");

	@Autowired
	private PublishSubscribeAmqpChannel channel;

	@Autowired
	private PollableAmqpChannel pollableWithEP;

	@Autowired
	private PointToPointSubscribableAmqpChannel withEP;

	@Autowired
	private PublishSubscribeAmqpChannel pubSubWithEP;

	@Autowired
	private PollableChannel out;

	@Autowired
	private CachingConnectionFactory connectionFactory;

	@Autowired
	private AmqpHeaderMapper mapperIn;

	@Autowired
	private AmqpHeaderMapper mapperOut;

	@After
	public void tearDown() {
		brokerIsRunning.deleteExchanges("si.fanout.foo", "si.fanout.channel", "si.fanout.pubSubWithEP");
		brokerIsRunning.removeTestQueues();
	}

	@Test
	public void pubSubLostConnectionTest() throws Exception {
		final CyclicBarrier latch = new CyclicBarrier(2);
		channel.subscribe(message -> {
			try {
				latch.await(10, TimeUnit.SECONDS);
			}
			catch (Exception e) {
			}
		});
		this.channel.send(new GenericMessage<>("foo"));
		latch.await(10, TimeUnit.SECONDS);
		latch.reset();
		BlockingQueueConsumer consumer = (BlockingQueueConsumer) TestUtils.getPropertyValue(this.channel,
				"container.consumers", Set.class).iterator().next();
		connectionFactory.destroy();
		waitForNewConsumer(this.channel, consumer);
		this.channel.send(new GenericMessage<>("bar"));
		latch.await(10, TimeUnit.SECONDS);
		this.channel.destroy();
		this.pubSubWithEP.destroy();
		this.withEP.destroy();
		this.pollableWithEP.destroy();
		assertThat(TestUtils.getPropertyValue(connectionFactory, "connectionListener.delegates", Collection.class)
				.size()).isEqualTo(0);
	}

	@SuppressWarnings("unchecked")
	private void waitForNewConsumer(PublishSubscribeAmqpChannel channel, BlockingQueueConsumer consumer)
			throws Exception {

		final Object consumersMonitor = TestUtils.getPropertyValue(channel, "container.consumersMonitor");
		int n = 0;
		while (n++ < 100) {
			Set<BlockingQueueConsumer> consumers = TestUtils
					.getPropertyValue(channel, "container.consumers", Set.class);
			synchronized (consumersMonitor) {
				if (!consumers.isEmpty()) {
					BlockingQueueConsumer newConsumer = consumers.iterator().next();
					if (newConsumer != consumer && newConsumer.getConsumerTags().size() > 0) {
						break;
					}
				}
			}
			Thread.sleep(100);
		}
		assertThat(n < 100).as("Failed to restart consumer").isTrue();
	}

	/*
	 * Verify queue is declared if not present and not declared if it is already present.
	 */
	@Test
	public void channelDeclarationTests() {
		RabbitAdmin admin = new RabbitAdmin(this.connectionFactory);
		admin.deleteQueue("implicit");
		SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(this.connectionFactory);
		container.setAutoStartup(false);
		AmqpTemplate amqpTemplate = mock(AmqpTemplate.class);
		PointToPointSubscribableAmqpChannel channel = new PointToPointSubscribableAmqpChannel("implicit", container,
				amqpTemplate);
		channel.setBeanFactory(mock(BeanFactory.class));
		channel.afterPropertiesSet();
		channel.onCreate(null);

		assertThat(admin.getQueueProperties("implicit")).isNotNull();

		admin.deleteQueue("explicit");
		channel.setQueueName("explicit");
		channel.afterPropertiesSet();
		channel.onCreate(null);

		assertThat(admin.getQueueProperties("explicit")).isNotNull();

		admin.deleteQueue("explicit");
		admin.declareQueue(new Queue("explicit", false)); // verify no declaration if exists with non-standard props
		channel.afterPropertiesSet();
		channel.onCreate(null);

		assertThat(admin.getQueueProperties("explicit")).isNotNull();
		admin.deleteQueue("explicit");
	}

	@Test
	public void testAmqpChannelFactoryBean() throws Exception {
		AmqpChannelFactoryBean channelFactoryBean = new AmqpChannelFactoryBean();
		channelFactoryBean.setBeanFactory(mock(BeanFactory.class));
		channelFactoryBean.setConnectionFactory(this.connectionFactory);
		channelFactoryBean.setBeanName("testChannel");
		channelFactoryBean.afterPropertiesSet();
		AbstractAmqpChannel channel = channelFactoryBean.getObject();
		assertThat(channel).isInstanceOf(PointToPointSubscribableAmqpChannel.class);

		channelFactoryBean = new AmqpChannelFactoryBean();
		channelFactoryBean.setBeanFactory(mock(BeanFactory.class));
		channelFactoryBean.setConnectionFactory(this.connectionFactory);
		channelFactoryBean.setBeanName("testChannel");
		channelFactoryBean.setPubSub(true);
		channelFactoryBean.afterPropertiesSet();
		channel = channelFactoryBean.getObject();
		assertThat(channel).isInstanceOf(PublishSubscribeAmqpChannel.class);

		RabbitAdmin rabbitAdmin = new RabbitAdmin(this.connectionFactory);
		rabbitAdmin.deleteQueue("testChannel");
		rabbitAdmin.deleteExchange("si.fanout.testChannel");
	}

	@Test
	public void extractPayloadTests() {
		Foo foo = new Foo("bar");
		Message<?> message = MessageBuilder.withPayload(foo).setHeader("baz", "qux").build();
		this.pollableWithEP.send(message);
		Message<?> received = this.pollableWithEP.receive(10000);
		assertThat(received).isNotNull();
		assertThat(received.getPayload()).isEqualTo(foo);
		assertThat(received.getHeaders().get("baz")).isEqualTo("qux");

		this.withEP.send(message);
		received = this.out.receive(10000);
		assertThat(received).isNotNull();
		assertThat(received.getPayload()).isEqualTo(foo);
		assertThat(received.getHeaders().get("baz")).isEqualTo("qux");

		this.pubSubWithEP.send(message);
		received = this.out.receive(10000);
		assertThat(received).isNotNull();
		assertThat(received.getPayload()).isEqualTo(foo);
		assertThat(received.getHeaders().get("baz")).isEqualTo("qux");

		assertThat(TestUtils.getPropertyValue(this.pollableWithEP, "inboundHeaderMapper")).isSameAs(this.mapperIn);
		assertThat(TestUtils.getPropertyValue(this.pollableWithEP, "outboundHeaderMapper")).isSameAs(this.mapperOut);
	}

	@Test
	public void messageConversionTests() {
		RabbitTemplate amqpTemplate = new RabbitTemplate(this.connectionFactory);
		MessageConverter messageConverter = mock(MessageConverter.class);
		amqpTemplate.setMessageConverter(messageConverter);
		PointToPointSubscribableAmqpChannel channel = new PointToPointSubscribableAmqpChannel("testConvertFail",
				new SimpleMessageListenerContainer(this.connectionFactory), amqpTemplate);
		channel.afterPropertiesSet();
		MessageListener listener = TestUtils.getPropertyValue(channel, "container.messageListener",
				MessageListener.class);
		willThrow(new MessageConversionException("foo", new IllegalStateException("bar")))
				.given(messageConverter).fromMessage(any(org.springframework.amqp.core.Message.class));
		assertThatExceptionOfType(MessageConversionException.class)
				.isThrownBy(() -> listener.onMessage(mock(org.springframework.amqp.core.Message.class)))
				.withCauseInstanceOf(IllegalStateException.class);
	}

	public static class Foo {

		private String bar;

		public Foo() {
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
				return other.bar == null;
			}
			else {
				return this.bar.equals(other.bar);
			}
		}

	}

}
