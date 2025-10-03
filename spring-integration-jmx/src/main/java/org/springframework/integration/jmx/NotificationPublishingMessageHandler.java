/*
 * Copyright 2002-present the original author or authors.
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

import javax.management.Notification;
import javax.management.ObjectName;

import org.springframework.integration.handler.AbstractMessageHandler;
import org.springframework.integration.mapping.OutboundMessageMapper;
import org.springframework.messaging.Message;

/**
 * An {@link AbstractMessageHandler} implementation to publish an incoming message
 * as a JMX {@link Notification}.
 * The {@link OutboundMessageMapper} is used to convert a {@link Message} to the {@link Notification}.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @author Trung Pham
 * @author Ngoc Nhan
 *
 * @since 2.0
 *
 * @deprecated since 7.0 in favor {@link org.springframework.integration.jmx.outbound.NotificationPublishingMessageHandler}
 */
@Deprecated(forRemoval = true, since = "7.0")
public class NotificationPublishingMessageHandler
		extends org.springframework.integration.jmx.outbound.NotificationPublishingMessageHandler {

	/**
	 * Construct an instance based on the provided object name.
	 * @param objectName the {@link ObjectName} to use for notification.
	 */
	public NotificationPublishingMessageHandler(ObjectName objectName) {
		super(objectName);
	}

	/**
	 * Construct an instance based on the provided object name.
	 * @param objectName the object name to use for notification.
	 */
	public NotificationPublishingMessageHandler(String objectName) {
		super(objectName);
	}

}
