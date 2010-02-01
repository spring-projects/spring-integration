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

import java.util.Map;

import javax.management.Notification;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.integration.core.Message;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.message.OutboundMessageMapper;
import org.springframework.jmx.export.MBeanExporter;
import org.springframework.jmx.export.notification.NotificationPublisher;
import org.springframework.jmx.export.notification.NotificationPublisherAware;
import org.springframework.jmx.support.ObjectNameManager;
import org.springframework.util.Assert;

/**
 * @author Mark Fisher
 * @since 2.0
 */
public class NotificationPublishingAdapter extends AbstractMessageHandler
		implements NotificationPublisherAware, BeanNameAware, BeanFactoryAware, InitializingBean {

	private volatile NotificationPublisher notificationPublisher;

	private volatile OutboundMessageMapper<Notification> notificationMapper;

	private volatile String objectName;

	private volatile String beanName;

	private volatile ListableBeanFactory beanFactory;


	public NotificationPublishingAdapter() {
		this(null);
	}

	public NotificationPublishingAdapter(String objectName) {
		this.objectName = objectName;
	}


	public void setNotificationMapper(OutboundMessageMapper<Notification> notificationMapper) {
		this.notificationMapper = notificationMapper;
	}

	public void setNotificationPublisher(NotificationPublisher notificationPublisher) {
		this.notificationPublisher = notificationPublisher;
	}

	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		Assert.isTrue(beanFactory instanceof ListableBeanFactory, "A ListableBeanFactory is required.");
		this.beanFactory = (ListableBeanFactory) beanFactory;
	}

	public void afterPropertiesSet() throws Exception {
		Map<String, MBeanExporter> exporters = BeanFactoryUtils.beansOfTypeIncludingAncestors(
				this.beanFactory, MBeanExporter.class);
		Assert.isTrue(exporters.size() == 1,
				"No unique MBeanExporter is available in the current context (found " +
				exporters.size() + ").");
		MBeanExporter exporter = exporters.values().iterator().next();
		if (this.objectName == null) {
			this.objectName = "org.springframework.integration:" +
					"type=notificationPublishingAdapter,name=" + this.beanName;
		}
		if (this.notificationMapper == null) {
			this.notificationMapper = new DefaultNotificationMapper(this.objectName);
		}
		exporter.registerManagedResource(this, ObjectNameManager.getInstance(this.objectName));
		if (this.logger.isInfoEnabled()) {
			this.logger.info("Registered NotificationPublishingAdapter as MBean with ObjectName: " + this.objectName);
		}
	}

	@Override
	protected void handleMessageInternal(Message<?> message) throws Exception {
		Assert.state(this.notificationPublisher != null, "NotificationPublisher must not be null.");
		this.notificationPublisher.sendNotification(this.notificationMapper.fromMessage(message));
	}

}
