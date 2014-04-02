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
package org.springframework.integration.ip.tcp.connection;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;

import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.messaging.Message;

/**
 * @author Gary Russell
 * @since 3.0
 *
 */
public class TcpConnectionEventListenerTests {

	@Test
	public void testNoFilter() {
		TcpConnectionEventListeningMessageProducer eventProducer = new TcpConnectionEventListeningMessageProducer();
		QueueChannel outputChannel = new QueueChannel();
		eventProducer.setOutputChannel(outputChannel);
		eventProducer.setBeanFactory(mock(BeanFactory.class));
		eventProducer.afterPropertiesSet();
		eventProducer.start();
		TcpConnectionSupport connection = Mockito.mock(TcpConnectionSupport.class);
		TcpConnectionEvent event1 = new TcpConnectionOpenEvent(connection, "foo");
		eventProducer.onApplicationEvent(event1);
		FooEvent event2 = new FooEvent(connection, "foo");
		eventProducer.onApplicationEvent(event2);
		BarEvent event3 = new BarEvent(connection, "foo");
		eventProducer.onApplicationEvent(event3);
		Message<?> message = outputChannel.receive(0);
		assertNotNull(message);
		assertSame(event1, message.getPayload());
		message = outputChannel.receive(0);
		assertNotNull(message);
		assertSame(event2, message.getPayload());
		message = outputChannel.receive(0);
		assertNotNull(message);
		assertSame(event3, message.getPayload());
		message = outputChannel.receive(0);
		assertNull(message);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void testFilter() {
		TcpConnectionEventListeningMessageProducer eventProducer = new TcpConnectionEventListeningMessageProducer();
		QueueChannel outputChannel = new QueueChannel();
		eventProducer.setOutputChannel(outputChannel);
		Class<?>[] eventTypes = new Class<?>[]{FooEvent.class, BarEvent.class};
		eventProducer.setEventTypes((Class<? extends TcpConnectionEvent>[]) eventTypes);
		eventProducer.setBeanFactory(mock(BeanFactory.class));
		eventProducer.afterPropertiesSet();
		eventProducer.start();
		TcpConnectionSupport connection = Mockito.mock(TcpConnectionSupport.class);
		TcpConnectionEvent event1 = new TcpConnectionOpenEvent(connection, "foo");
		eventProducer.onApplicationEvent(event1);
		FooEvent event2 = new FooEvent(connection, "foo");
		eventProducer.onApplicationEvent(event2);
		BarEvent event3 = new BarEvent(connection, "foo");
		eventProducer.onApplicationEvent(event3);
		Message<?> message = outputChannel.receive(0);
		assertNotNull(message);
		assertSame(event2, message.getPayload());
		message = outputChannel.receive(0);
		assertNotNull(message);
		assertSame(event3, message.getPayload());
		message = outputChannel.receive(0);
		assertNull(message);
	}

	@SuppressWarnings("serial")
	private class FooEvent extends TcpConnectionOpenEvent {

		public FooEvent(TcpConnectionSupport connection, String connectionFactoryName) {
			super(connection, connectionFactoryName);
		}

	}

	@SuppressWarnings("serial")
	private class BarEvent extends TcpConnectionOpenEvent {

		public BarEvent(TcpConnectionSupport connection, String connectionFactoryName) {
			super(connection, connectionFactoryName);
		}

	}

}
