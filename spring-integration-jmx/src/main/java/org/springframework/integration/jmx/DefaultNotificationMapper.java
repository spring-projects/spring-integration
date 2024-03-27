/*
 * Copyright 2002-2024 the original author or authors.
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
