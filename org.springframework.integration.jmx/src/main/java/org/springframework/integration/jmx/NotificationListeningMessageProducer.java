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
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.support.ComponentMetadata;
import org.springframework.util.Assert;

/**
 * A JMX {@link NotificationListener} implementation that will send Messages
 * containing the JMX {@link Notification} instances as their payloads.
 * 
 * @author Mark Fisher
 * @since 2.0
 */
public class NotificationListeningMessageProducer extends MessageProducerSupport implements NotificationListener {

	private final Log logger = LogFactory.getLog(this.getClass());

	private volatile MBeanServer server;

	private volatile Set<ObjectName> objectNames;

	private volatile NotificationFilter filter;

	private volatile Object handback;


	/**
	 * Provide a reference to the MBeanServer where the notification
	 * publishing MBeans are registered. 
	 */
	public void setServer(MBeanServer server) {
		this.server = server;
	}

	/**
	 * Specify one or more JMX ObjectNames of notification publishers
 	 * to which this notification listener should be subscribed.
	 */
	public void setObjectNames(ObjectName... objectNames) {
		this.objectNames = new LinkedHashSet<ObjectName>(Arrays.asList(objectNames));
	}

	/**
	 * Specify a {@link NotificationFilter} to be passed to the server
	 * when registering this listener. The filter may be null.
	 */
	public void setFilter(NotificationFilter filter) {
		this.filter = filter;
	}

	/**
	 * Specify a handback object to provide context to the listener
	 * upon notification. This object may be null.
	 */
	public void setHandback(Object handback) {
		this.handback = handback;
	}

	/**
	 * Notification handling method implementation. Creates a Message with the
	 * JMX {@link Notification} as its payload, and if the handback object is
	 * not null, it sets that as a Message header value. The Message is then
	 * sent to this producer's output channel.
	 */
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
	protected void populateComponentMetadata(ComponentMetadata metadata) {
		metadata.setComponentType("notification-listener");
		metadata.setAttribute("transport", "jmx");
	}

	/**
	 * Registers the notification listener with the specified ObjectNames.
	 */
	@Override
	protected void doStart() {
		try {
			Assert.notNull(this.server, "MBeanServer is required.");
			Assert.notEmpty(this.objectNames, "One or more ObjectNames are required.");
			for (ObjectName objectName : this.objectNames) {
				this.server.addNotificationListener(objectName, this, this.filter, this.handback);
			}
		}
		catch (InstanceNotFoundException e) {
			throw new IllegalStateException("Failed to find MBean instance.", e); 
		}
	}

	/**
	 * Unregisters the notification listener.
	 */
	@Override
	protected void doStop() {
		try {
			Assert.notNull(this.server, "MBeanServer is required.");
			Assert.notEmpty(this.objectNames, "One or more ObjectNames are required.");
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
