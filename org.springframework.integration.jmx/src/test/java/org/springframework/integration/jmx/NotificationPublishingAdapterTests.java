/*
 * Copyright 2002-2010 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;

import javax.management.Notification;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import org.springframework.context.support.StaticApplicationContext;
import org.springframework.integration.message.StringMessage;
import org.springframework.jmx.export.MBeanExporter;
import org.springframework.jmx.support.ObjectNameManager;

/**
 * @author Mark Fisher
 * @since 2.0
 */
public class NotificationPublishingAdapterTests {

	private final StaticApplicationContext context = new StaticApplicationContext();

	private final TestNotificationListener listener = new TestNotificationListener();

	private volatile ObjectName publisherObjectName;


	@Before
	public void setup() throws Exception {
		this.publisherObjectName = ObjectNameManager.getInstance(
				"org.springframework.integration:type=notificationPublishingAdapter,name=testPublisher");
		context.registerSingleton("exporter", MBeanExporter.class);
		context.registerSingleton("testPublisher", NotificationPublishingAdapter.class);
		context.refresh();
		MBeanExporter exporter = context.getBean(MBeanExporter.class);
		exporter.getServer().addNotificationListener(this.publisherObjectName, this.listener, null, null);
	}

	@After
	public void cleanup() {
		this.listener.clearNotifications();
	}


	@Test
	public void simplePublish() {
		NotificationPublishingAdapter adapter = context.getBean(NotificationPublishingAdapter.class);
		assertEquals(0, this.listener.notifications.size());
		adapter.handleMessage(new StringMessage("foo"));
		assertEquals(1, this.listener.notifications.size());
		Notification notification = this.listener.notifications.get(0);
		assertEquals(this.publisherObjectName, notification.getSource());
		assertEquals("foo", notification.getMessage());
		assertEquals("SpringIntegrationNotification", notification.getType());
	}


	public static class TestNotificationListener implements NotificationListener {

		private final List<Notification> notifications = new ArrayList<Notification>();

		public void handleNotification(Notification notification, Object handback) {
			this.notifications.add(notification);
		}

		void clearNotifications() {
			this.notifications.clear();
		}
	}

}
