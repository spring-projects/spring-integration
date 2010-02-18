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

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.management.InstanceNotFoundException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanServer;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageHistoryEvent;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.util.Assert;

/**
 * @author Mark Fisher
 * @since 2.0
 */
public class NotificationListeningAdapter extends MessageProducerSupport implements NotificationListener {

	private final Log logger = LogFactory.getLog(this.getClass());

	private volatile MBeanServer server;

	private volatile Set<ObjectName> objectNames;

	private volatile NotificationFilter filter;

	private volatile Object handback;


	public void setServer(MBeanServer server) {
		this.server = server;
	}

	public void setObjectNames(ObjectName... objectNames) {
		this.objectNames = new LinkedHashSet<ObjectName>(Arrays.asList(objectNames));
	}

	public void setFilter(NotificationFilter filter) {
		this.filter = filter;
	}

	public void setHandback(Object handback) {
		this.handback = handback;
	}

	public void handleNotification(Notification notification, Object handback) {
		if (logger.isInfoEnabled()) {
			logger.info("received notification: " + notification + ", and handback: " + handback);
		}
		MessageBuilder<?> builder = MessageBuilder.withPayload(notification);
		if (handback != null) {
			builder.setHeader(JmxHeaders.NOTIFICATION_HANDBACK, handback);
		}
		Message<?> message = builder.build();
		this.sendMessage(message);
	}

	@Override
	protected void postProcessHistoryEvent(MessageHistoryEvent event) {
		event.setComponentType("notification-listener");
		event.setProperty("transport", "jmx");
	}

	@Override
	protected void doStart() {
		try {
			Assert.notNull(this.server, "MBeanServer is required.");
			for (ObjectName objectName : this.objectNames) {
				this.server.addNotificationListener(objectName, this, this.filter, this.handback);
			}
		}
		catch (InstanceNotFoundException e) {
			throw new IllegalStateException("Failed to find MBean instance.", e); 
		}
	}

	@Override
	protected void doStop() {
		try {
			Assert.notNull(this.server, "MBeanServer is required.");
			for (ObjectName objectName : this.objectNames) {
				this.server.removeNotificationListener(objectName, this, this.filter, this.handback);
			}
		}
		catch (InstanceNotFoundException e) {
			throw new IllegalStateException("Failed to find MBean instance.", e); 
		}
		catch (ListenerNotFoundException e) {
			throw new IllegalStateException("Failed to find NotificationListener.", e); 
		}
	}

}
