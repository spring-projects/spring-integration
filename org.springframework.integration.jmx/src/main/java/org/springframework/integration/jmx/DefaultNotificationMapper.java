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

import java.util.concurrent.atomic.AtomicInteger;

import javax.management.MalformedObjectNameException;
import javax.management.Notification;
import javax.management.ObjectName;

import org.springframework.integration.core.Message;
import org.springframework.integration.message.OutboundMessageMapper;
import org.springframework.jmx.support.ObjectNameManager;

/**
 * @author Mark Fisher
 * @since 2.0
 */
class DefaultNotificationMapper implements OutboundMessageMapper<Notification> {

	private final ObjectName sourceObjectName;

	private volatile String defaultNotificationType = "SpringIntegrationNotification";

	private final AtomicInteger sequence = new AtomicInteger();


	DefaultNotificationMapper(ObjectName sourceObjectName) {
		this.sourceObjectName = sourceObjectName;
	}

	DefaultNotificationMapper(String sourceObjectName) {
		this(stringToObjectName(sourceObjectName));
	}


	public void setDefaultNotificationType(String defaultNotificationType) {
		this.defaultNotificationType = defaultNotificationType;
	}

	public Notification fromMessage(Message<?> message) throws Exception {
		String type = this.resolveNotificationType(message);
		return new Notification(type, this.sourceObjectName, this.sequence.incrementAndGet(),
				message.getPayload().toString());
	}

	private String resolveNotificationType(Message<?> message) {
		String type = message.getHeaders().get(JmxHeaders.NOTIFICATION_TYPE, String.class);
		return (type != null) ? type : this.defaultNotificationType;
	}

	private static ObjectName stringToObjectName(String objectName) {
		try {
			return ObjectNameManager.getInstance(objectName);
		}
		catch (MalformedObjectNameException e) {
			throw new IllegalArgumentException(e);
		}
	}

}
