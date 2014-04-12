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

import java.util.Map;

import javax.management.MalformedObjectNameException;
import javax.management.Notification;
import javax.management.ObjectName;

import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.mapping.OutboundMessageMapper;
import org.springframework.integration.monitor.IntegrationMBeanExporter;
import org.springframework.jmx.export.MBeanExporter;
import org.springframework.jmx.export.annotation.ManagedResource;
import org.springframework.jmx.export.notification.NotificationPublisher;
import org.springframework.jmx.export.notification.NotificationPublisherAware;
import org.springframework.jmx.support.ObjectNameManager;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class NotificationPublishingMessageHandler extends AbstractMessageHandler implements BeanFactoryAware, InitializingBean {

	private final PublisherDelegate delegate = new PublisherDelegate();

	private volatile OutboundMessageMapper<Notification> notificationMapper;

	private final ObjectName objectName;

	private volatile String defaultNotificationType;


	public NotificationPublishingMessageHandler(ObjectName objectName) {
		Assert.notNull(objectName, "JMX ObjectName is required");
		this.objectName = objectName;
	}

	public NotificationPublishingMessageHandler(String objectName) {
		Assert.notNull(objectName, "JMX ObjectName is required");
		try {
			this.objectName = ObjectNameManager.getInstance(objectName);
		}
		catch (MalformedObjectNameException e) {
			throw new IllegalArgumentException(e);
		}
	}


	/**
	 * Set a mapper for creating Notifications from a Message. If not provided,
	 * a default implementation will be used such that String-typed payloads will be
	 * passed as the 'message' of the Notification and all other payload types
	 * will be passed as the 'userData' of the Notification.
	 *
	 * @param notificationMapper The notification mapper.
	 */
	public void setNotificationMapper(OutboundMessageMapper<Notification> notificationMapper) {
		this.notificationMapper = notificationMapper;
	}

	/**
	 * Specify a dot-delimited String representing the Notification type to
	 * use by default when <em>no</em> explicit Notification mapper
	 * has been configured. If not provided, then a notification type header will
	 * be required for each message being mapped into a Notification.
	 *
	 * @param defaultNotificationType The default notification type.
	 */
	public void setDefaultNotificationType(String defaultNotificationType) {
		this.defaultNotificationType = defaultNotificationType;
	}

	@Override
	public String getComponentType() {
		return "jmx:notification-publishing-channel-adapter";
	}

	@Override
	public final void onInit() throws Exception {
		Assert.isTrue(this.getBeanFactory() instanceof ListableBeanFactory, "A ListableBeanFactory is required.");
		Map<String, MBeanExporter> exporters = BeanFactoryUtils.beansOfTypeIncludingAncestors(
				(ListableBeanFactory) this.getBeanFactory(), MBeanExporter.class);
		Assert.isTrue(exporters.size() > 0, "No MBeanExporter is available in the current context.");
		MBeanExporter exporter = null;
		for (MBeanExporter exp : exporters.values()) {
			exporter = exp;
			if (exporter instanceof IntegrationMBeanExporter) {
				break;
			}
		}
		if (this.notificationMapper == null) {
			this.notificationMapper = new DefaultNotificationMapper(this.objectName, this.defaultNotificationType);
		}
		exporter.registerManagedResource(this.delegate, this.objectName);
		if (this.logger.isInfoEnabled()) {
			this.logger.info("Registered JMX notification publisher as MBean with ObjectName: " + this.objectName);
		}
	}

	@Override
	protected void handleMessageInternal(Message<?> message) throws Exception {
		this.delegate.publish(this.notificationMapper.fromMessage(message));
	}


	/**
	 * Simple class used for the actual MBean instances to be registered.
	 */
	@ManagedResource
	private static class PublisherDelegate implements NotificationPublisherAware {

		private volatile NotificationPublisher notificationPublisher;

		@Override
		public void setNotificationPublisher(NotificationPublisher notificationPublisher) {
			this.notificationPublisher = notificationPublisher;
		}

		private void publish(Notification notification) {
			Assert.state(this.notificationPublisher != null, "NotificationPublisher must not be null.");
			this.notificationPublisher.sendNotification(notification);
		}
	}

}
