/*
 * Copyright 2014-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.amqp.dsl;

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.integration.amqp.outbound.AmqpOutboundEndpoint;
import org.springframework.util.Assert;

/**
 * @author Artem Bilan
 * @since 5.0
 */
public class AmqpOutboundEndpointSpec
		extends AmqpBaseOutboundEndpointSpec<AmqpOutboundEndpointSpec, AmqpOutboundEndpoint> {

	private final boolean expectReply;

	AmqpOutboundEndpointSpec(AmqpTemplate amqpTemplate, boolean expectReply) {
		this.expectReply = expectReply;
		this.target = new AmqpOutboundEndpoint(amqpTemplate);
		this.target.setExpectReply(expectReply);
		this.target.setHeaderMapper(this.headerMapper);
		if (expectReply) {
			this.target.setRequiresReply(true);
		}
	}

	@Override
	public AmqpOutboundEndpointSpec mappedReplyHeaders(String... headers) {
		Assert.isTrue(this.expectReply, "'mappedReplyHeaders' can be applied only for gateway");
		return super.mappedReplyHeaders(headers);
	}

}
