/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.integration.jmx;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.management.InstanceNotFoundException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanServerConnection;
import javax.management.Notification;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;

import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.integration.endpoint.MessageProducerSupport;
import org.springframework.integration.support.AbstractIntegrationMessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * A JMX {@link NotificationListener} implementation that will send Messages
 * containing the JMX {@link Notification} instances as their payloads.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class NotificationListeningMessageProducer extends MessageProducerSupport
		implements NotificationListener, ApplicationListener<ContextRefreshedEvent> {

	private final AtomicBoolean listenerRegisteredOnStartup = new AtomicBoolean();

	private MBeanServerConnection server;

	private ObjectName[] mBeanObjectNames;

	private NotificationFilter filter;

	private Object handback;


	/**
	 * Provide a reference to the MBeanServer where the notification
	 * publishing MBeans are registered.
	 * @param server the MBean server connection.
	 */
	public void setServer(MBeanServerConnection server) {
		this.server = server;
	}

	/**
	 * Specify the JMX ObjectNames (or patterns)
	 * of the notification publisher
	 * to which this notification listener should be subscribed.
	 * @param objectNames The object names.
	 */
	public void setObjectName(ObjectName... objectNames) {
		Assert.notEmpty(objectNames, "'objectNames' must contain at least one ObjectName");
		this.mBeanObjectNames = Arrays.copyOf(objectNames, objectNames.length);
	}

	/**
	 * Specify a {@link NotificationFilter} to be passed to the server
	 * when registering this listener. The filter may be null.
	 * @param filter The filter.
	 */
	public void setFilter(NotificationFilter filter) {
		this.filter = filter;
	}

	/**
	 * Specify a handback object to provide context to the listener
	 * upon notification. This object may be null.
	 * @param handback The object.
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
	@Override
	public void handleNotification(Notification notification, Object handback) {
		this.logger.info(() -> "received notification: " + notification + ", and handback: " + handback);
		AbstractIntegrationMessageBuilder<?> builder = getMessageBuilderFactory().withPayload(notification);
		if (handback != null) {
			builder.setHeader(JmxHeaders.NOTIFICATION_HANDBACK, handback);
		}
		Message<?> message = builder.build();
		sendMessage(message);
	}

	@Override
	public String getComponentType() {
		return "jmx:notification-listening-channel-adapter";
	}

	/**
	 * The {@link NotificationListener} might not be registered on {@link #start()}
	 * because the {@code MBeanExporter} might not been started yet.
	 * @param event the ContextRefreshedEvent event
	 */
	@Override
	public void onApplicationEvent(ContextRefreshedEvent event) {
		if (!this.listenerRegisteredOnStartup.getAndSet(true) && isAutoStartup()) {
			doStart();
		}
	}

	/**
	 * Registers the notification listener with the specified ObjectNames.
	 */
	@Override
	protected void doStart() {
		if (!this.listenerRegisteredOnStartup.get()) {
			return;
		}
		this.logger.debug("Registering to receive notifications");
		Assert.notNull(this.server, "MBeanServer is required.");
		Assert.notNull(this.mBeanObjectNames, "An ObjectName is required.");
		try {
			Collection<ObjectName> objectNames = this.retrieveMBeanNames();
			if (objectNames.size() < 1) {
				this.logger.error(() -> "No MBeans found matching ObjectName pattern(s): " +
						Arrays.toString(this.mBeanObjectNames));
			}
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
		this.logger.debug("Unregistering notifications");
		if (this.server != null && this.mBeanObjectNames != null) {
			Collection<ObjectName> objectNames = this.retrieveMBeanNames();
			for (ObjectName objectName : objectNames) {
				try {
					this.server.removeNotificationListener(objectName, this, this.filter, this.handback);
				}
				catch (InstanceNotFoundException ex) {
					this.logger.error(ex, "Failed to find MBean instance.");
				}
				catch (ListenerNotFoundException ex) {
					this.logger.error(ex, "Failed to find NotificationListener.");
				}
				catch (IOException ex) {
					this.logger.error(ex, "IOException on MBeanServerConnection.");
				}
			}
		}
	}

	protected Collection<ObjectName> retrieveMBeanNames() {
		List<ObjectName> objectNames = new ArrayList<>();
		for (ObjectName pattern : this.mBeanObjectNames) {
			Set<ObjectInstance> mBeanInfos;
			try {
				mBeanInfos = this.server.queryMBeans(pattern, null);
			}
			catch (IOException e) {
				throw new IllegalStateException("IOException on MBeanServerConnection.", e);
			}
			if (mBeanInfos.size() == 0) {
				this.logger.debug(() -> "No MBeans found matching pattern: " + pattern);
			}
			for (ObjectInstance instance : mBeanInfos) {
				this.logger.debug(() -> "Found MBean: " + instance.getObjectName().toString());
				objectNames.add(instance.getObjectName());
			}
		}
		return objectNames;
	}

}
