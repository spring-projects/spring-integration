/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.jmx;

import java.util.ArrayList;
import java.util.List;

import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.support.StaticApplicationContext;
import org.springframework.integration.monitor.IntegrationMBeanExporter;
import org.springframework.jmx.export.MBeanExporter;
import org.springframework.jmx.support.MBeanServerFactoryBean;
import org.springframework.jmx.support.ObjectNameManager;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.support.GenericMessage;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class NotificationPublishingMessageHandlerTests {

	private final StaticApplicationContext context = new StaticApplicationContext();

	private final TestNotificationListener listener = new TestNotificationListener();

	private volatile ObjectName publisherObjectName;

	@Before
	public void setup() throws Exception {
		this.publisherObjectName = ObjectNameManager.getInstance("test:type=publisher");
		this.context.registerBean("mbeanServer", MBeanServerFactoryBean.class, MBeanServerFactoryBean::new);
		this.context.registerSingleton("exporter", IntegrationMBeanExporter.class,
				new MutablePropertyValues()
						.add("server", new RuntimeBeanReference("mbeanServer")));
		this.context.registerSingleton("anotherExporter", MBeanExporter.class);

		RootBeanDefinition publisherDefinition = new RootBeanDefinition(NotificationPublishingMessageHandler.class);
		publisherDefinition.getConstructorArgumentValues().addGenericArgumentValue(this.publisherObjectName);
		publisherDefinition.getPropertyValues().add("defaultNotificationType", "test.type");
		context.registerBeanDefinition("testPublisher", publisherDefinition);
		context.refresh();
		MBeanExporter exporter = context.getBean(IntegrationMBeanExporter.class);
		exporter.getServer().addNotificationListener(publisherObjectName, this.listener, null, null);
	}

	@After
	public void cleanup() {
		this.listener.clearNotifications();
		context.close();
	}

	@Test
	public void simplePublish() {
		MessageHandler handler = context.getBean("testPublisher", MessageHandler.class);
		assertThat(this.listener.notifications.size()).isEqualTo(0);
		handler.handleMessage(new GenericMessage<String>("foo"));
		assertThat(this.listener.notifications.size()).isEqualTo(1);
		Notification notification = this.listener.notifications.get(0);
		assertThat(notification.getSource()).isEqualTo(this.publisherObjectName);
		assertThat(notification.getMessage()).isEqualTo("foo");
		assertThat(notification.getType()).isEqualTo("test.type");
	}

	public static class TestNotificationListener implements NotificationListener {

		private final List<Notification> notifications = new ArrayList<>();

		public void handleNotification(Notification notification, Object handback) {
			this.notifications.add(notification);
		}

		void clearNotifications() {
			this.notifications.clear();
		}

	}

}
