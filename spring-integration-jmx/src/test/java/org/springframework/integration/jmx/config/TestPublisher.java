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

package org.springframework.integration.jmx.config;

import javax.management.Notification;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.jmx.export.notification.NotificationPublisher;
import org.springframework.jmx.export.notification.NotificationPublisherAware;

/**
 * @author Mark Fisher
 * @since 2.0
 */
@ManagedResource
public class TestPublisher implements NotificationPublisherAware, BeanNameAware {

	private volatile NotificationPublisher notificationPublisher;

	private String beanName;

	public void setNotificationPublisher(NotificationPublisher notificationPublisher) {
		this.notificationPublisher = notificationPublisher;
	}

	public void setBeanName(String name) {
		this.beanName = name;
	}

	public void send(String s) {
		Notification notification = new Notification("test.type",
				"org.springframework.integration.jmx.config:type=TestPublisher,name=" + this.beanName, 1, s);
		this.notificationPublisher.sendNotification(notification);
	}

}
