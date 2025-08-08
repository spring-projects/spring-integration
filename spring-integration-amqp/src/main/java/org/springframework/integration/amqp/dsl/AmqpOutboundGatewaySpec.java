/*
 * Copyright © 2020 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2020-present the original author or authors.
 */

package org.springframework.integration.amqp.dsl;

import org.springframework.amqp.core.AmqpTemplate;

/**
 * Spec for an outbound AMQP gateway.
 *
 * @author Gary Russell
 * @since 5.3
 *
 */
public class AmqpOutboundGatewaySpec extends AmqpOutboundEndpointSpec<AmqpOutboundGatewaySpec> {

	protected AmqpOutboundGatewaySpec(AmqpTemplate amqpTemplate) {
		super(amqpTemplate, true);
	}

}
