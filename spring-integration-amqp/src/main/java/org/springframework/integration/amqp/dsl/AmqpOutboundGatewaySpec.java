/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
