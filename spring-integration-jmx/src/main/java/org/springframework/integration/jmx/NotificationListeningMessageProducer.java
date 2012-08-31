/*
 * Copyright 2002-2012 the original author or authors.
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import javax.management.InstanceNotFoundException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.integration.Message;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

/**
 * A JMX {@link NotificationListener} implementation that will send Messages
 * containing the JMX {@link Notification} instances as their payloads.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @since 2.0
 */
public class NotificationListeningMessageProducer extends MessageProducerSupport implements NotificationListener {

	private final Log logger = LogFactory.getLog(this.getClass());

	private volatile MBeanServerConnection server;

	private volatile ObjectName[] objectNames;

	private volatile NotificationFilter filter;

	private volatile Object handback;


	/**
	 * Provide a reference to the MBeanServer where the notification
	 * publishing MBeans are registered.
	 */
	public void setServer(MBeanServerConnection server) {
		this.server = server;
	}

	/**
	 * Specify the JMX ObjectName (or pattern)
	 * of the notification publisher
 	 * to which this notification listener should be subscribed.
	 */
	public void setObjectName(ObjectName objectName) {
		Assert.notNull(objectName, "'objectName' must not be null");
		this.objectNames = new ObjectName[] { objectName };
	}

	/**
	 * Specify the JMX ObjectNames (or patterns)
	 * of the notification publishers
	 * to which this notification listener should be subscribed.
	 */
	public void setObjectNames(Collection<ObjectName> objectNames) {
		Assert.isTrue(!CollectionUtils.isEmpty(objectNames), "'objectNames' must contain at least one ObjectName");
		this.objectNames = objectNames.toArray(new ObjectName[objectNames.size()]);
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
	public String getComponentType() {
		return "jmx:notification-listening-channel-adapter";
	}

	/**
	 * Registers the notification listener with the specified ObjectNames.
	 */
	@Override
	protected void doStart() {
		try {
			Assert.notNull(this.server, "MBeanServer is required.");
			Assert.notNull(this.objectNames, "An ObjectName is required.");
			Collection<ObjectName> objectNames = this.retrieveMBeanNames();
			for (ObjectName objectName : objectNames) {
				this.server.addNotificationListener(objectName, this, this.filter, this.handback);
			}
		}
		catch (InstanceNotFoundException e) {
			throw new IllegalStateException("Failed to find MBean instance.", e);
		}
		catch (IOException e) {
			throw new IllegalStateException("IOException on MBeanServerConnection.", e);
		}
	}

	/**
	 * Unregisters the notification listener.
	 */
	@Override
	protected void doStop() {
		if (this.server != null && this.objectNames != null) {
			Collection<ObjectName> objectNames = this.retrieveMBeanNames();
			for (ObjectName objectName : objectNames) {
				try {
					this.server.removeNotificationListener(objectName, this, this.filter, this.handback);
				}
				catch (InstanceNotFoundException e) {
					logger.error("Failed to find MBean instance.", e);
				}
				catch (ListenerNotFoundException e) {
					logger.error("Failed to find NotificationListener.", e);
				}
				catch (IOException e) {
					logger.error("IOException on MBeanServerConnection.", e);
				}
			}
		}
	}

	protected Collection<ObjectName> retrieveMBeanNames() {
		List<ObjectName> objectNames = new ArrayList<ObjectName>();
		for (ObjectName pattern : this.objectNames) {
			Set<ObjectInstance> mBeanInfos;
			try {
				mBeanInfos = this.server.queryMBeans(pattern, null);
			}
			catch (IOException e) {
				throw new IllegalStateException("IOException on MBeanServerConnection.", e);
			}
			for (ObjectInstance instance : mBeanInfos) {
				objectNames.add(instance.getObjectName());
			}
		}
		return objectNames;
	}
}
