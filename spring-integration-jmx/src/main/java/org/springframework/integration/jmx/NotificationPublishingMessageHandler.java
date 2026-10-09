/*
 * Copyright 2002-present the original author or authors.
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
