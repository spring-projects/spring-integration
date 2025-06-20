/*
 * Copyright 2016-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
