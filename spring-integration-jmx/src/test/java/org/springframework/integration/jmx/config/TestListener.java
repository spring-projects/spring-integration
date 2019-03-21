/*
 * Copyright 2002-2019 the original author or authors.
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
