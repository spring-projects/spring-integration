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

package org.springframework.integration.jms.config;

import java.util.Properties;

import org.junit.jupiter.api.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.NotReadablePropertyException;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.jms.inbound.JmsMessageDrivenEndpoint;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Michael Bannister
 * @author Gary Russell
 * @author Glenn Renfro
 */
public class JmsMessageDrivenChannelAdapterParserTests {

	long timeoutOnReceive = 300000;

	@Test
	public void adapterWithMessageSelector() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsInboundWithMessageSelector.xml", this.getClass());
		PollableChannel output = (PollableChannel) context.getBean("output2");
		Message<?> message = output.receive(timeoutOnReceive);
		MessageHistory history = MessageHistory.read(message);
		assertThat(history).isNotNull();
		Properties componentHistoryRecord = TestUtils.locateComponentInHistory(history, "messageDrivenAdapter", 0);
		assertThat(componentHistoryRecord).isNotNull();
		JmsMessageDrivenEndpoint endpoint = context.getBean("messageDrivenAdapter", JmsMessageDrivenEndpoint.class);
		assertThat(componentHistoryRecord.get("type")).isEqualTo("jms:message-driven-channel-adapter");
		assertThat(message).as("message should not be null").isNotNull();
		assertThat(message.getPayload()).isEqualTo("test [with selector: TestProperty = 'foo']");
		endpoint.stop();
		context.close();
	}

	@Test
	public void adapterWithPubSubDomain() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsInboundWithPubSubDomain.xml", this.getClass());
		JmsMessageDrivenEndpoint endpoint = context.getBean("messageDrivenAdapter", JmsMessageDrivenEndpoint.class);
		AbstractMessageListenerContainer container = TestUtils.getPropertyValue(endpoint, "listenerContainer");
		assertThat(container.isPubSubDomain()).isEqualTo(Boolean.TRUE);
		assertThat(container.isSubscriptionDurable()).isFalse();
		assertThat(container.getDurableSubscriptionName()).isNull();
		endpoint.stop();
		context.close();
	}

	@Test
	public void adapterWithDurableSubscription() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsInboundWithDurableSubscription.xml", this.getClass());
		JmsMessageDrivenEndpoint endpoint = context.getBean("messageDrivenAdapter", JmsMessageDrivenEndpoint.class);
		DefaultMessageListenerContainer container = TestUtils.getPropertyValue(endpoint, "listenerContainer");
		assertThat(container.isPubSubDomain()).isEqualTo(Boolean.TRUE);
		assertThat(container.isSubscriptionDurable()).isEqualTo(Boolean.TRUE);
		assertThat(container.getDurableSubscriptionName()).isEqualTo("testDurableSubscriptionName");
		assertThat(container.getClientId()).isEqualTo("testClientId");
		assertThat(container.isSubscriptionShared()).isTrue();
		endpoint.stop();
		context.close();
	}

	@Test
	public void adapterWithTaskExecutor() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsInboundWithTaskExecutor.xml", this.getClass());
		JmsMessageDrivenEndpoint endpoint =
				context.getBean("messageDrivenAdapter.adapter", JmsMessageDrivenEndpoint.class);
		DefaultMessageListenerContainer container = TestUtils.getPropertyValue(endpoint, "listenerContainer");
		assertThat(TestUtils.<Object>getPropertyValue(container, "taskExecutor")).isSameAs(context.getBean("exec"));
		endpoint.stop();
		context.close();
	}

	@Test
	public void testAdapterWithReceiveTimeout() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsInboundWithContainerSettings.xml", this.getClass());
		JmsMessageDrivenEndpoint adapter =
				context.getBean("adapterWithReceiveTimeout.adapter", JmsMessageDrivenEndpoint.class);
		adapter.start();
		assertThat(TestUtils.<Long>getPropertyValue(adapter, "listenerContainer.receiveTimeout")).isEqualTo(1111L);
		adapter.stop();
		context.close();
	}

	@Test
	public void testAdapterWithRecoveryInterval() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsInboundWithContainerSettings.xml", this.getClass());
		JmsMessageDrivenEndpoint adapter =
				context.getBean("adapterWithRecoveryInterval.adapter", JmsMessageDrivenEndpoint.class);
		adapter.start();
		AbstractMessageListenerContainer container = (AbstractMessageListenerContainer)
				new DirectFieldAccessor(adapter).getPropertyValue("listenerContainer");
		Object recoveryInterval;
		try {
			recoveryInterval = TestUtils.getPropertyValue(container, "recoveryInterval");
		}
		catch (NotReadablePropertyException e) {
			recoveryInterval = TestUtils.getPropertyValue(container, "backOff.interval");
		}
		assertThat(recoveryInterval).isEqualTo(2222L);
		adapter.stop();
		context.close();
	}

	@Test
	public void testAdapterWithIdleTaskExecutionLimit() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsInboundWithContainerSettings.xml", this.getClass());
		JmsMessageDrivenEndpoint adapter =
				context.getBean("adapterWithIdleTaskExecutionLimit.adapter", JmsMessageDrivenEndpoint.class);
		adapter.start();
		assertThat(TestUtils.<Integer>getPropertyValue(adapter, "listenerContainer.idleTaskExecutionLimit"))
				.isEqualTo(7);
		adapter.stop();
		context.close();
	}

	@Test
	public void testAdapterWithIdleConsumerLimit() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsInboundWithContainerSettings.xml", this.getClass());
		JmsMessageDrivenEndpoint adapter =
				context.getBean("adapterWithIdleConsumerLimit.adapter", JmsMessageDrivenEndpoint.class);
		adapter.start();
		AbstractMessageListenerContainer container = (AbstractMessageListenerContainer)
				new DirectFieldAccessor(adapter).getPropertyValue("listenerContainer");
		assertThat(new DirectFieldAccessor(container).getPropertyValue("idleConsumerLimit")).isEqualTo(33);
		assertThat(new DirectFieldAccessor(container).getPropertyValue("cacheLevel")).isEqualTo(3);
		adapter.stop();
		context.close();
	}

	@Test
	public void testAdapterWithContainerClass() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsInboundWithContainerClass.xml", this.getClass());
		JmsMessageDrivenEndpoint adapter =
				context.getBean("adapterWithIdleConsumerLimit.adapter", JmsMessageDrivenEndpoint.class);
		MessageChannel channel = context.getBean("adapterWithIdleConsumerLimit", MessageChannel.class);
		assertThat(TestUtils.<Object>getPropertyValue(adapter, "listener.gatewayDelegate.requestChannel"))
				.isSameAs(channel);
		adapter.start();
		FooContainer container = TestUtils.getPropertyValue(adapter, "listenerContainer");
		assertThat(context.getBean("adapterWithIdleConsumerLimit.container")).isSameAs(container);
		assertThat(new DirectFieldAccessor(container).getPropertyValue("idleConsumerLimit")).isEqualTo(33);
		assertThat(new DirectFieldAccessor(container).getPropertyValue("cacheLevel")).isEqualTo(3);
		assertThat(TestUtils.<Object>getPropertyValue(container, "messageListener"))
				.isSameAs(context.getBean("adapterWithIdleConsumerLimit.listener"));
		adapter.stop();

		adapter = context.getBean("adapterWithIdleConsumerLimit2.adapter", JmsMessageDrivenEndpoint.class);
		channel = context.getBean("adapterWithIdleConsumerLimit2", MessageChannel.class);
		assertThat(TestUtils.<Object>getPropertyValue(adapter, "listener.gatewayDelegate.requestChannel"))
				.isSameAs(channel);
		adapter.start();
		container = TestUtils.getPropertyValue(adapter, "listenerContainer");
		assertThat(context.getBean("adapterWithIdleConsumerLimit2.container")).isSameAs(container);
		assertThat(new DirectFieldAccessor(container).getPropertyValue("idleConsumerLimit")).isEqualTo(33);
		assertThat(new DirectFieldAccessor(container).getPropertyValue("cacheLevel")).isEqualTo(3);
		adapter.stop();

		adapter = context.getBean("org.springframework.integration.jms.inbound.JmsMessageDrivenEndpoint#0",
				JmsMessageDrivenEndpoint.class);
		channel = context.getBean("in", MessageChannel.class);
		assertThat(TestUtils.<Object>getPropertyValue(adapter, "listener.gatewayDelegate.requestChannel"))
				.isSameAs(channel);
		adapter.start();
		container = TestUtils.getPropertyValue(adapter, "listenerContainer");
		assertThat(context.getBean("org.springframework.integration.jms.inbound.JmsMessageDrivenEndpoint#0.container"))
				.isSameAs(container);
		assertThat(new DirectFieldAccessor(container).getPropertyValue("idleConsumerLimit")).isEqualTo(33);
		assertThat(new DirectFieldAccessor(container).getPropertyValue("cacheLevel")).isEqualTo(3);
		adapter.stop();
		context.close();
	}

	public static final class FooContainer extends DefaultMessageListenerContainer {

	}

}
