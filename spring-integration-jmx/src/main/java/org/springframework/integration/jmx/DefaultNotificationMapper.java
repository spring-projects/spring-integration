/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jmx;

import java.util.concurrent.atomic.AtomicLong;

import javax.management.Notification;
import javax.management.ObjectName;

import org.springframework.integration.mapping.OutboundMessageMapper;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.util.Assert;

/**
 * Default Messaging Mapper implementation for the {@link NotificationPublishingMessageHandler}.
 * If the Message has a String-typed payload, that will be passed as the 'message' of
 * the Notification instance. Otherwise, the payload object will be passed as the
 * 'userData' of the Notification instance.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 *
 * @since 2.0
 */
class DefaultNotificationMapper implements OutboundMessageMapper<Notification> {

	private final ObjectName sourceObjectName;

	@Nullable
	private final String defaultNotificationType;

	private final AtomicLong sequence = new AtomicLong();

	DefaultNotificationMapper(ObjectName sourceObjectName, @Nullable String defaultNotificationType) {
		this.sourceObjectName = sourceObjectName;
		this.defaultNotificationType = defaultNotificationType;
	}

	public Notification fromMessage(Message<?> message) {
		String type = resolveNotificationType(message);
		Assert.hasText(type, "No notification type header is available, and no default has been provided.");
		Object payload = message.getPayload();
		String notificationMessage = (payload instanceof String) ? (String) payload : null;
		Notification notification = new Notification(type, this.sourceObjectName,
				this.sequence.incrementAndGet(), System.currentTimeMillis(), notificationMessage);
		if (!(payload instanceof String)) {
			notification.setUserData(payload);
		}
		return notification;
	}

	private String resolveNotificationType(Message<?> message) {
		String type = message.getHeaders().get(JmxHeaders.NOTIFICATION_TYPE, String.class);
		return (type != null) ? type : this.defaultNotificationType;
	}

}
