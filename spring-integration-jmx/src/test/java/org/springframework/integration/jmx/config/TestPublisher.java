/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
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
