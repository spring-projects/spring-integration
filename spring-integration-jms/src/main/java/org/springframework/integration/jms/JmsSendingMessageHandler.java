/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jms;

import org.springframework.jms.core.JmsTemplate;

/**
 * A MessageConsumer that sends the converted Message payload within a JMS Message.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 *
 * @deprecated since 7.0 in favor of {@link org.springframework.integration.jms.outbound.JmsSendingMessageHandler}
 */
@Deprecated(forRemoval = true, since = "7.0")
public class JmsSendingMessageHandler extends org.springframework.integration.jms.outbound.JmsSendingMessageHandler {

	public JmsSendingMessageHandler(JmsTemplate jmsTemplate) {
		super(jmsTemplate);
	}

}
