/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
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
