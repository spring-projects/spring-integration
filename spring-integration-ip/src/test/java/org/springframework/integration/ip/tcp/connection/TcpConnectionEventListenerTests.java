/*
 * Copyright 2002-2022 the original author or authors.
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

package org.springframework.integration.ip.tcp.connection;

import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.core.ResolvableType;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.event.inbound.ApplicationEventListeningMessageProducer;
import org.springframework.messaging.Message;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * @author Gary Russell
 * @author Artem Bilan
 * @since 3.0
 *
 */
public class TcpConnectionEventListenerTests {

	@Test
	public void testNoFilter() {
		ApplicationEventListeningMessageProducer eventProducer = new ApplicationEventListeningMessageProducer();
		QueueChannel outputChannel = new QueueChannel();
		eventProducer.setOutputChannel(outputChannel);
		eventProducer.setEventTypes(TcpConnectionEvent.class);
		BeanFactory mock = mock(BeanFactory.class);
		given(mock.getBean(AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME,
				ApplicationEventMulticaster.class))
				.willReturn(mock(ApplicationEventMulticaster.class));
		eventProducer.setBeanFactory(mock);
		eventProducer.afterPropertiesSet();
		eventProducer.start();
		TcpConnectionSupport connection = Mockito.mock(TcpConnectionSupport.class);

		assertThat(eventProducer.supportsEventType(ResolvableType.forClass(TcpConnectionOpenEvent.class))).isTrue();
		TcpConnectionEvent event1 = new TcpConnectionOpenEvent(connection, "foo");
		eventProducer.onApplicationEvent(event1);

		assertThat(eventProducer.supportsEventType(ResolvableType.forClass(FooEvent.class))).isTrue();
		FooEvent event2 = new FooEvent(connection, "foo");
		eventProducer.onApplicationEvent(event2);

		assertThat(eventProducer.supportsEventType(ResolvableType.forClass(BarEvent.class))).isTrue();
		BarEvent event3 = new BarEvent(connection, "foo");
		eventProducer.onApplicationEvent(event3);

		Message<?> message = outputChannel.receive(0);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isSameAs(event1);
		message = outputChannel.receive(0);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isSameAs(event2);
		message = outputChannel.receive(0);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isSameAs(event3);
		message = outputChannel.receive(0);
		assertThat(message).isNull();
	}

	@Test
	public void testFilter() {
		ApplicationEventListeningMessageProducer eventProducer = new ApplicationEventListeningMessageProducer();
		QueueChannel outputChannel = new QueueChannel();
		eventProducer.setOutputChannel(outputChannel);
		eventProducer.setEventTypes(FooEvent.class, BarEvent.class);
		BeanFactory mock = mock(BeanFactory.class);
		given(mock.getBean(AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME,
				ApplicationEventMulticaster.class))
				.willReturn(mock(ApplicationEventMulticaster.class));
		eventProducer.setBeanFactory(mock);
		eventProducer.afterPropertiesSet();
		eventProducer.start();
		TcpConnectionSupport connection = Mockito.mock(TcpConnectionSupport.class);

		assertThat(eventProducer.supportsEventType(ResolvableType.forClass(TcpConnectionOpenEvent.class))).isFalse();

		assertThat(eventProducer.supportsEventType(ResolvableType.forClass(FooEvent.class))).isTrue();
		FooEvent event2 = new FooEvent(connection, "foo");
		eventProducer.onApplicationEvent(event2);

		assertThat(eventProducer.supportsEventType(ResolvableType.forClass(BarEvent.class))).isTrue();
		BarEvent event3 = new BarEvent(connection, "foo");
		eventProducer.onApplicationEvent(event3);

		Message<?> message = outputChannel.receive(0);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isSameAs(event2);
		message = outputChannel.receive(0);
		assertThat(message).isNotNull();
		assertThat(message.getPayload()).isSameAs(event3);
		message = outputChannel.receive(0);
		assertThat(message).isNull();
	}

	@SuppressWarnings("serial")
	private class FooEvent extends TcpConnectionOpenEvent {

		FooEvent(TcpConnectionSupport connection, String connectionFactoryName) {
			super(connection, connectionFactoryName);
		}

	}

	@SuppressWarnings("serial")
	private class BarEvent extends TcpConnectionOpenEvent {

		BarEvent(TcpConnectionSupport connection, String connectionFactoryName) {
			super(connection, connectionFactoryName);
		}

	}

}
