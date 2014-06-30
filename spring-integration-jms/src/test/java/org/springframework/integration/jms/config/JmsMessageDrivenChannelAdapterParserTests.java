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

package org.springframework.integration.jms.config;

import static org.junit.Assert.*;

import java.util.Properties;

import org.junit.Test;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.NotReadablePropertyException;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.integration.history.MessageHistory;
import org.springframework.integration.jms.JmsMessageDrivenEndpoint;
import org.springframework.integration.test.util.TestUtils;
import org.springframework.jms.listener.AbstractMessageListenerContainer;
import org.springframework.jms.listener.DefaultMessageListenerContainer;
import org.springframework.jms.support.destination.JmsDestinationAccessor;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.PollableChannel;

/**
 * @author Mark Fisher
 * @author Michael Bannister
 * @author Gary Russell
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
		assertNotNull(history);
		Properties componentHistoryRecord = TestUtils.locateComponentInHistory(history, "messageDrivenAdapter", 0);
		assertNotNull(componentHistoryRecord);
		JmsMessageDrivenEndpoint endpoint =  context.getBean("messageDrivenAdapter", JmsMessageDrivenEndpoint.class);
		assertEquals("jms:message-driven-channel-adapter", componentHistoryRecord.get("type"));
		assertNotNull("message should not be null", message);
		assertEquals("test [with selector: TestProperty = 'foo']", message.getPayload());
		endpoint.stop();
		context.close();
	}

	@Test
	public void adapterWithPubSubDomain() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsInboundWithPubSubDomain.xml", this.getClass());
		JmsMessageDrivenEndpoint endpoint = context.getBean("messageDrivenAdapter", JmsMessageDrivenEndpoint.class);
		JmsDestinationAccessor container = (JmsDestinationAccessor) new DirectFieldAccessor(endpoint).getPropertyValue("listenerContainer");
		assertEquals(Boolean.TRUE, container.isPubSubDomain());
		endpoint.stop();
		context.close();
	}

	@Test
	public void adapterWithDurableSubscription() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsInboundWithDurableSubscription.xml", this.getClass());
		JmsMessageDrivenEndpoint endpoint = context.getBean("messageDrivenAdapter", JmsMessageDrivenEndpoint.class);
		DefaultMessageListenerContainer container = (DefaultMessageListenerContainer) new DirectFieldAccessor(endpoint).getPropertyValue("listenerContainer");
		assertEquals(Boolean.TRUE, container.isPubSubDomain());
		assertEquals(Boolean.TRUE, container.isSubscriptionDurable());
		assertEquals("testDurableSubscriptionName", container.getDurableSubscriptionName());
		assertEquals("testClientId", container.getClientId());
		endpoint.stop();
		context.close();
	}

	@Test
	public void adapterWithTaskExecutor() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsInboundWithTaskExecutor.xml", this.getClass());
		JmsMessageDrivenEndpoint endpoint = context.getBean("messageDrivenAdapter.adapter", JmsMessageDrivenEndpoint.class);
		DefaultMessageListenerContainer container = TestUtils.getPropertyValue(endpoint, "listenerContainer",
				DefaultMessageListenerContainer.class);
		assertSame(context.getBean("exec"), TestUtils.getPropertyValue(container, "taskExecutor"));
		endpoint.stop();
		context.close();
	}

	@Test
	public void testAdapterWithReceiveTimeout() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsInboundWithContainerSettings.xml", this.getClass());
		JmsMessageDrivenEndpoint adapter = (JmsMessageDrivenEndpoint) context.getBean("adapterWithReceiveTimeout.adapter");
		adapter.start();
		AbstractMessageListenerContainer container = (AbstractMessageListenerContainer)
				new DirectFieldAccessor(adapter).getPropertyValue("listenerContainer");
		assertEquals(1111L, new DirectFieldAccessor(container).getPropertyValue("receiveTimeout"));
		adapter.stop();
		context.close();
	}

	@Test
	public void testAdapterWithRecoveryInterval() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsInboundWithContainerSettings.xml", this.getClass());
		JmsMessageDrivenEndpoint adapter = (JmsMessageDrivenEndpoint) context.getBean("adapterWithRecoveryInterval.adapter");
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
		assertEquals(2222L, recoveryInterval);
		adapter.stop();
		context.close();
	}

	@Test
	public void testAdapterWithIdleTaskExecutionLimit() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsInboundWithContainerSettings.xml", this.getClass());
		JmsMessageDrivenEndpoint adapter = (JmsMessageDrivenEndpoint) context.getBean("adapterWithIdleTaskExecutionLimit.adapter");
		adapter.start();
		AbstractMessageListenerContainer container = (AbstractMessageListenerContainer)
				new DirectFieldAccessor(adapter).getPropertyValue("listenerContainer");
		assertEquals(7, new DirectFieldAccessor(container).getPropertyValue("idleTaskExecutionLimit"));
		adapter.stop();
		context.close();
	}

	@Test
	public void testAdapterWithIdleConsumerLimit() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsInboundWithContainerSettings.xml", this.getClass());
		JmsMessageDrivenEndpoint adapter = (JmsMessageDrivenEndpoint) context.getBean("adapterWithIdleConsumerLimit.adapter");
		adapter.start();
		AbstractMessageListenerContainer container = (AbstractMessageListenerContainer)
				new DirectFieldAccessor(adapter).getPropertyValue("listenerContainer");
		assertEquals(33, new DirectFieldAccessor(container).getPropertyValue("idleConsumerLimit"));
		assertEquals(3, new DirectFieldAccessor(container).getPropertyValue("cacheLevel"));
		adapter.stop();
		context.close();
	}

	@Test
	public void testAdapterWithContainerClass() {
		ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
				"jmsInboundWithContainerClass.xml", this.getClass());
		JmsMessageDrivenEndpoint adapter = context.getBean("adapterWithIdleConsumerLimit.adapter", JmsMessageDrivenEndpoint.class);
		MessageChannel channel = context.getBean("adapterWithIdleConsumerLimit", MessageChannel.class);
		assertSame(channel, TestUtils.getPropertyValue(adapter, "listener.gatewayDelegate.requestChannel"));
		adapter.start();
		FooContainer container = TestUtils.getPropertyValue(adapter, "listenerContainer", FooContainer.class);
		assertEquals(33, new DirectFieldAccessor(container).getPropertyValue("idleConsumerLimit"));
		assertEquals(3, new DirectFieldAccessor(container).getPropertyValue("cacheLevel"));
		adapter.stop();
		context.close();
	}

	public static final class FooContainer extends DefaultMessageListenerContainer {

	}

}
