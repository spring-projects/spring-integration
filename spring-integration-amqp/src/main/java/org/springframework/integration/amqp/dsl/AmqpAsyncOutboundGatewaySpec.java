/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.amqp.dsl;

import org.springframework.amqp.rabbit.AsyncRabbitTemplate;
import org.springframework.integration.amqp.outbound.AsyncAmqpOutboundGateway;

/**
 * @author Artem Bilan
 * @since 5.0
 */
public class AmqpAsyncOutboundGatewaySpec
		extends AmqpBaseOutboundEndpointSpec<AmqpAsyncOutboundGatewaySpec, AsyncAmqpOutboundGateway> {

	protected AmqpAsyncOutboundGatewaySpec(AsyncRabbitTemplate template) {
		this.target = new AsyncAmqpOutboundGateway(template);
		this.target.setRequiresReply(true);
	}

}
