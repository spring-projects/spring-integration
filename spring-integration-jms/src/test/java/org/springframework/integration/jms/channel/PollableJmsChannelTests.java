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

package org.springframework.integration.jms.channel;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import jakarta.jms.DeliveryMode;
import jakarta.jms.Destination;
import jakarta.jms.TextMessage;
import org.apache.activemq.artemis.jms.client.ActiveMQQueue;
import org.apache.activemq.artemis.reader.MessageUtil;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import org.springframework.integration.jms.ActiveMQMultiContextTests;
import org.springframework.integration.jms.config.JmsChannelFactoryBean;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.jms.connection.CachingConnectionFactory;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.JmsHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @author Gunnar Hillert
 * @author Artem Bilan
 */
public class PollableJmsChannelTests extends ActiveMQMultiContextTests {

	@Test
	public void queueReference() throws Exception {
		Destination queue = new ActiveMQQueue("pollableJmsChannelTestQueue");

		JmsChannelFactoryBean factoryBean = new JmsChannelFactoryBean(false);
		CachingConnectionFactory ccf = new CachingConnectionFactory(connectionFactory);
		ccf.setCacheConsumers(false);
		factoryBean.setConnectionFactory(ccf);
		factoryBean.setDestination(queue);
		factoryBean.setBeanFactory(mock());
		factoryBean.afterPropertiesSet();
		PollableJmsChannel channel = (PollableJmsChannel) factoryBean.getObject();
		boolean sent1 = channel.send(new GenericMessage<>("test1"));
		assertThat(sent1).isTrue();
		boolean sent2 = channel.send(new GenericMessage<>("test2"));
		assertThat(sent2).isTrue();
		Message<?> result1 = channel.receive(10000);
		assertThat(result1).isNotNull();
		assertThat(result1.getPayload()).isEqualTo("test1");
		assertThat(result1.getHeaders())
				.containsEntry(MessageUtil.JMSXDELIVERYCOUNT, 1)
				.containsEntry(JmsHeaders.DESTINATION, queue);
		Message<?> result2 = channel.receive(10000);
		assertThat(result2).isNotNull();
		assertThat(result2.getPayload()).isEqualTo("test2");
	}

	@Test
	public void queueName() throws Exception {
		JmsChannelFactoryBean factoryBean = new JmsChannelFactoryBean(false);
		CachingConnectionFactory ccf = new CachingConnectionFactory(connectionFactory);
		ccf.setCacheConsumers(false);
		factoryBean.setConnectionFactory(ccf);
		factoryBean.setDestinationName("someDynamicQueue");
		factoryBean.setPubSubDomain(false);
		factoryBean.setBeanFactory(mock());
		factoryBean.afterPropertiesSet();
		PollableJmsChannel channel = (PollableJmsChannel) factoryBean.getObject();
		boolean sent1 = channel.send(new GenericMessage<>("test1"));
		assertThat(sent1).isTrue();
		boolean sent2 = channel.send(new GenericMessage<>("test2"));
		assertThat(sent2).isTrue();
		Message<?> result1 = channel.receive(10000);
		assertThat(result1).isNotNull();
		assertThat(result1.getPayload()).isEqualTo("test1");
		Message<?> result2 = channel.receive(10000);
		assertThat(result2).isNotNull();
		assertThat(result2.getPayload()).isEqualTo("test2");
	}

	@Test
	public void queueNameWithFalsePreReceiveInterceptors() throws Exception {
		JmsChannelFactoryBean factoryBean = new JmsChannelFactoryBean(false);
		CachingConnectionFactory ccf = new CachingConnectionFactory(connectionFactory);
		ccf.setCacheConsumers(false);
		factoryBean.setConnectionFactory(ccf);
		factoryBean.setDestinationName("someDynamicQueue");
		factoryBean.setPubSubDomain(false);
		List<ChannelInterceptor> interceptorList = new ArrayList<>();
		ChannelInterceptor interceptor = spy(new SampleInterceptor(false));
		interceptorList.add(interceptor);
		factoryBean.setInterceptors(interceptorList);
		factoryBean.setBeanFactory(mock());
		factoryBean.afterPropertiesSet();
		PollableJmsChannel channel = (PollableJmsChannel) factoryBean.getObject();
		boolean sent1 = channel.send(new GenericMessage<>("test1"));
		assertThat(sent1).isTrue();
		Message<?> result1 = channel.receive(10000);
		assertThat(result1).isNull();
		verify(interceptor, times(1)).preReceive(Mockito.any(MessageChannel.class));
		verify(interceptor, times(0)).postReceive(Mockito.any(Message.class), Mockito.any(MessageChannel.class));
	}

	@Test
	public void queueNameWithTruePreReceiveInterceptors() throws Exception {
		JmsChannelFactoryBean factoryBean = new JmsChannelFactoryBean(false);
		CachingConnectionFactory ccf = new CachingConnectionFactory(connectionFactory);
		ccf.setCacheConsumers(false);
		factoryBean.setConnectionFactory(ccf);
		factoryBean.setDestinationName("someDynamicQueue");
		factoryBean.setPubSubDomain(false);
		List<ChannelInterceptor> interceptorList = new ArrayList<>();
		ChannelInterceptor interceptor = spy(new SampleInterceptor(true));
		interceptorList.add(interceptor);
		factoryBean.setInterceptors(interceptorList);
		factoryBean.setBeanFactory(mock());
		factoryBean.afterPropertiesSet();
		PollableJmsChannel channel = (PollableJmsChannel) factoryBean.getObject();
		boolean sent1 = channel.send(new GenericMessage<>("test1"));
		assertThat(sent1).isTrue();
		Message<?> result1 = channel.receive(10000);
		assertThat(result1).isNotNull();
		verify(interceptor, times(1)).preReceive(Mockito.any(MessageChannel.class));
		verify(interceptor, times(1)).postReceive(Mockito.any(Message.class), Mockito.any(MessageChannel.class));
	}

	@Test
	public void qos() throws Exception {
		Destination queue = new ActiveMQQueue("pollableJmsChannelTestQueue");
		CachingConnectionFactory ccf = new CachingConnectionFactory(connectionFactory);
		ccf.setCacheConsumers(false);

		JmsChannelFactoryBean factoryBean = new JmsChannelFactoryBean(false);
		factoryBean.setConnectionFactory(ccf);
		factoryBean.setDestination(queue);
		factoryBean.setExplicitQosEnabled(true);
		factoryBean.setPriority(5);
		int ttl = 10000;
		factoryBean.setTimeToLive(ttl);
		factoryBean.setDeliveryPersistent(false);
		factoryBean.setBeanFactory(mock());
		factoryBean.afterPropertiesSet();
		PollableJmsChannel channel = (PollableJmsChannel) factoryBean.getObject();
		final JmsTemplate receiver = new JmsTemplate(connectionFactory);
		boolean sent1 = channel.send(new GenericMessage<>("test1"));
		assertThat(sent1).isTrue();
		final AtomicReference<jakarta.jms.Message> message = new AtomicReference<>();
		final CountDownLatch latch1 = new CountDownLatch(1);
		ExecutorService exec = Executors.newSingleThreadExecutor();
		exec.execute(() -> {
			message.set(receiver.receive(queue));
			latch1.countDown();
		});
		assertThat(latch1.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(message.get()).isNotNull();
		assertThat(message.get().getJMSPriority()).isEqualTo(5);
		assertThat(message.get().getJMSExpiration() <= System.currentTimeMillis() + ttl).isTrue();
		assertThat(message.get().getJMSDeliveryMode()).isEqualTo(DeliveryMode.NON_PERSISTENT);
		message.set(null);
		final CountDownLatch latch2 = new CountDownLatch(1);
		boolean sent2 = channel.send(MessageBuilder.withPayload("test1").setPriority(6).build());
		assertThat(sent2).isTrue();
		exec.execute(() -> {
			message.set(receiver.receive(queue));
			latch2.countDown();
		});
		assertThat(latch2.await(10, TimeUnit.SECONDS)).isTrue();
		assertThat(message.get()).isNotNull();
		assertThat(message.get().getJMSPriority()).isEqualTo(6);
		assertThat(message.get().getJMSExpiration() <= System.currentTimeMillis() + ttl).isTrue();
		assertThat(message.get().getJMSDeliveryMode()).isEqualTo(DeliveryMode.NON_PERSISTENT);
		exec.shutdownNow();
	}

	@Test
	public void selector() throws Exception {
		Destination queue = new ActiveMQQueue("pollableJmsChannelSelectorTestQueue");

		JmsChannelFactoryBean factoryBean = new JmsChannelFactoryBean(false);
		CachingConnectionFactory ccf = new CachingConnectionFactory(connectionFactory);
		ccf.setCacheConsumers(false);
		factoryBean.setConnectionFactory(ccf);
		factoryBean.setDestination(queue);

		factoryBean.setMessageSelector("property='value'");

		factoryBean.setBeanFactory(mock());
		factoryBean.afterPropertiesSet();
		PollableJmsChannel channel = (PollableJmsChannel) factoryBean.getObject();
		boolean sent1 = channel.send(new GenericMessage<>("test1"));
		assertThat(sent1).isTrue();
		Message<?> result1 = channel.receive(100);
		assertThat(result1).isNull();

		JmsTemplate jmsTemplate = new JmsTemplate(connectionFactory);
		jmsTemplate.setDefaultDestinationName("pollableJmsChannelSelectorTestQueue");
		jmsTemplate.send(session -> {
			TextMessage message = session.createTextMessage("test2");
			message.setStringProperty("property", "value");
			return message;
		});

		Message<?> result2 = channel.receive(10000);
		assertThat(result2).isNotNull();
		assertThat(result2.getPayload()).isEqualTo("test2");
		assertThat(result2.getHeaders()).containsEntry("property", "value");
	}

	public record SampleInterceptor(boolean preReceiveFlag) implements ChannelInterceptor {

		@Override
		public boolean preReceive(MessageChannel channel) {
			return this.preReceiveFlag;
		}

	}

}
