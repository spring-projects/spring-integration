/*
 * Copyright © 2020 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2020-present the original author or authors.
 */

package org.springframework.integration.amqp.dsl;

import org.springframework.amqp.core.AmqpTemplate;

/**
 * Spec for an outbound AMQP channel adapter.
 *
 * @author Gary Russell
 * @author Artme Bilan
 *
 * @since 5.3
 *
 */
public class AmqpOutboundChannelAdapterSpec extends AmqpOutboundEndpointSpec<AmqpOutboundChannelAdapterSpec> {

	protected AmqpOutboundChannelAdapterSpec(AmqpTemplate amqpTemplate) {
		super(amqpTemplate, false);
	}

	/**
	 * If true, and the message payload is an {@link Iterable} of {@link org.springframework.messaging.Message},
	 * send the messages in a single invocation of the template (same channel) and optionally
	 * wait for the confirms or die.
	 * @param multiSend true to send multiple messages.
	 * @return the spec.
	 */
	public AmqpOutboundChannelAdapterSpec multiSend(boolean multiSend) {
		this.target.setMultiSend(multiSend);
		return this;
	}

}
