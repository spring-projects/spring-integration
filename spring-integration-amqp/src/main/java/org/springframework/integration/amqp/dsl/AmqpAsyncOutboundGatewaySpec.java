/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
