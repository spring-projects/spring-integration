/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jmx.config;

import javax.management.MBeanServer;
import javax.management.Notification;
import javax.management.NotificationListener;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.jmx.support.ObjectNameManager;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 * @since 2.0
 */
public class TestListener implements NotificationListener, BeanFactoryAware {

	Notification lastNotification;

	public void setBeanFactory(BeanFactory beanFactory) {
		MBeanServer server = beanFactory.getBean("mbeanServer", MBeanServer.class);
		try {
			server.addNotificationListener(
					ObjectNameManager.getInstance("test.publisher:name=publisher"), this, null, null);
			server.addNotificationListener(
					ObjectNameManager.getInstance("test.publisher:name=publisher-chain"), this, null, null);
		}
		catch (Exception e) {
			throw new IllegalArgumentException(e);
		}
	}

	public void handleNotification(Notification notification, Object handback) {
		this.lastNotification = notification;
	}

}
