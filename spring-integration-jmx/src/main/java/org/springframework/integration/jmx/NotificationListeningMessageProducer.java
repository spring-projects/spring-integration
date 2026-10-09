/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jmx;

import javax.management.Notification;
import javax.management.NotificationListener;

/**
 * A JMX {@link NotificationListener} implementation that will send Messages
 * containing the JMX {@link Notification} instances as their payloads.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 *
 * @deprecated since 7.0 in favor {@link org.springframework.integration.jmx.inbound.NotificationListeningMessageProducer}
 */
@Deprecated(forRemoval = true, since = "7.0")
public class NotificationListeningMessageProducer extends
		org.springframework.integration.jmx.inbound.NotificationListeningMessageProducer {

}
