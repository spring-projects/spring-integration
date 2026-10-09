/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jms;

/**
 * JMS MessageListener that converts a JMS Message into a Spring Integration
 * Message and sends that Message to a channel. If the 'expectReply' value is
 * <code>true</code>, it will also wait for a Spring Integration reply Message
 * and convert that into a JMS reply.
 *
 * @author Mark Fisher
 * @author Juergen Hoeller
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @author Gary Russell
 * @author Ngoc Nhan
 *
 * @deprecated since 7.0 in favor of {@link org.springframework.integration.jms.inbound.ChannelPublishingJmsMessageListener}
 */
@Deprecated(forRemoval = true, since = "7.0")
public class ChannelPublishingJmsMessageListener
		extends org.springframework.integration.jms.inbound.ChannelPublishingJmsMessageListener {

}
