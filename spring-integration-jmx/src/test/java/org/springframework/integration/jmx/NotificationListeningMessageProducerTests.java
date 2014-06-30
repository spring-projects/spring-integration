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

package org.springframework.integration.jmx;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.concurrent.atomic.AtomicInteger;

import javax.management.MBeanServer;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.ObjectName;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.jmx.export.MBeanExporter;
import org.springframework.jmx.export.notification.NotificationPublisher;
import org.springframework.jmx.export.notification.NotificationPublisherAware;
import org.springframework.jmx.support.MBeanServerFactoryBean;
import org.springframework.jmx.support.ObjectNameManager;
import org.springframework.messaging.Message;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 * @since 2.0
 */
public class NotificationListeningMessageProducerTests {

	private volatile MBeanServer server;

	private volatile ObjectName objectName;

	private final NumberHolder numberHolder = new NumberHolder();


	@Before
	public void setup() throws Exception {
		MBeanServerFactoryBean serverFactoryBean = new MBeanServerFactoryBean();
		serverFactoryBean.setLocateExistingServerIfPossible(true);
		serverFactoryBean.afterPropertiesSet();
		this.server = serverFactoryBean.getObject();
		MBeanExporter exporter = new MBeanExporter();
		exporter.setAutodetect(false);
		exporter.afterPropertiesSet();
		this.objectName = ObjectNameManager.getInstance("si:name=numberHolder");
		exporter.registerManagedResource(this.numberHolder, this.objectName);
	}

	@After
	public void cleanup() throws Exception {
		this.server.unregisterMBean(this.objectName);
	}

	@Test
	public void simpleNotification() throws Exception {
		QueueChannel outputChannel = new QueueChannel();
		NotificationListeningMessageProducer adapter = new NotificationListeningMessageProducer();
		adapter.setServer(this.server);
		adapter.setObjectName(this.objectName);
		adapter.setOutputChannel(outputChannel);
		adapter.setBeanFactory(mock(BeanFactory.class));
		adapter.afterPropertiesSet();
		adapter.start();
		adapter.onApplicationEvent(new ContextRefreshedEvent(Mockito.mock(ApplicationContext.class)));
		this.numberHolder.publish("foo");
		Message<?> message = outputChannel.receive(0);
		assertNotNull(message);
		assertTrue(message.getPayload() instanceof Notification);
		Notification notification = (Notification) message.getPayload();
		assertEquals("foo", notification.getMessage());
		assertEquals(objectName, notification.getSource());
		assertNull(message.getHeaders().get(JmxHeaders.NOTIFICATION_HANDBACK));
	}

	@Test
	public void notificationWithHandback() throws Exception {
		QueueChannel outputChannel = new QueueChannel();
		NotificationListeningMessageProducer adapter = new NotificationListeningMessageProducer();
		adapter.setServer(this.server);
		adapter.setObjectName(this.objectName);
		adapter.setOutputChannel(outputChannel);
		Integer handback = 123;
		adapter.setHandback(handback);
		adapter.setBeanFactory(mock(BeanFactory.class));
		adapter.afterPropertiesSet();
		adapter.start();
		adapter.onApplicationEvent(new ContextRefreshedEvent(Mockito.mock(ApplicationContext.class)));
		this.numberHolder.publish("foo");
		Message<?> message = outputChannel.receive(0);
		assertNotNull(message);
		assertTrue(message.getPayload() instanceof Notification);
		Notification notification = (Notification) message.getPayload();
		assertEquals("foo", notification.getMessage());
		assertEquals(objectName, notification.getSource());
		assertEquals(handback, message.getHeaders().get(JmxHeaders.NOTIFICATION_HANDBACK));
	}

	@Test
	@SuppressWarnings("serial")
	public void notificationWithFilter() throws Exception {
		QueueChannel outputChannel = new QueueChannel();
		NotificationListeningMessageProducer adapter = new NotificationListeningMessageProducer();
		adapter.setServer(this.server);
		adapter.setObjectName(this.objectName);
		adapter.setOutputChannel(outputChannel);
		adapter.setFilter(new NotificationFilter() {
			@Override
			public boolean isNotificationEnabled(Notification notification) {
				return !notification.getMessage().equals("bad");
			}
		});
		adapter.setBeanFactory(mock(BeanFactory.class));
		adapter.afterPropertiesSet();
		adapter.start();
		adapter.onApplicationEvent(new ContextRefreshedEvent(Mockito.mock(ApplicationContext.class)));
		this.numberHolder.publish("bad");
		Message<?> message = outputChannel.receive(0);
		assertNull(message);
		this.numberHolder.publish("okay");
		message = outputChannel.receive(0);
		assertNotNull(message);
		assertTrue(message.getPayload() instanceof Notification);
		Notification notification = (Notification) message.getPayload();
		assertEquals("okay", notification.getMessage());
	}


	public static class NumberHolder implements NotificationPublisherAware {

		private final AtomicInteger number = new AtomicInteger();

		private final AtomicInteger sequence = new AtomicInteger();

		private volatile NotificationPublisher notificationPublisher;

		public int getNumber() {
			return this.number.get();
		}

		public void setNumber(int value) {
			this.number.set(value);
		}

		@Override
		public void setNotificationPublisher(NotificationPublisher notificationPublisher) {
			this.notificationPublisher = notificationPublisher;
		}

		public void publish(String message) {
			Notification notification = new Notification("testType", this, sequence.getAndIncrement(), message);
			this.notificationPublisher.sendNotification(notification);
		}
	}

}
