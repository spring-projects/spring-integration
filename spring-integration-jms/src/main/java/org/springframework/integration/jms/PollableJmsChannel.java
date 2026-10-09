/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jms;

import org.springframework.jms.core.JmsTemplate;

/**
 * A JMS-backed channel from which messages can be received through polling.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @author Ngoc Nhan
 *
 * @since 2.0
 *
 * @deprecated since 7.0 in favor of {@link org.springframework.integration.jms.channel.PollableJmsChannel}
 */
@Deprecated(forRemoval = true, since = "7.0")
public class PollableJmsChannel extends org.springframework.integration.jms.channel.PollableJmsChannel {

	public PollableJmsChannel(JmsTemplate jmsTemplate) {
		super(jmsTemplate);
	}

}
