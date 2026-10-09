/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jms;

import org.springframework.jms.core.JmsTemplate;

/**
 * A source for receiving JMS Messages with a polling listener. This source is
 * only recommended for very low message volume. Otherwise, the
 * {@link JmsMessageDrivenEndpoint} that uses Spring's MessageListener container
 * support is a better option.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 *
 * @deprecated since 7.0 in favor of {@link org.springframework.integration.jms.inbound.JmsDestinationPollingSource}
 */
@Deprecated(forRemoval = true, since = "7.0")
public class JmsDestinationPollingSource
		extends org.springframework.integration.jms.inbound.JmsDestinationPollingSource {

	public JmsDestinationPollingSource(JmsTemplate jmsTemplate) {
		super(jmsTemplate);
	}

}
