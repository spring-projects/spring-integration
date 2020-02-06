/*
 * Copyright 2014-2020 the original author or authors.
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

import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.integration.amqp.outbound.AmqpOutboundEndpoint;
import org.springframework.util.Assert;

/**
 * Base spec for outbound AMQP endpoints.
 *
 * @param <S> the spec subclass type.
 *
 * @author Artem Bilan
 * @since 5.0
 */
public abstract class AmqpOutboundEndpointSpec<S extends AmqpOutboundEndpointSpec<S>>
		extends AmqpBaseOutboundEndpointSpec<S, AmqpOutboundEndpoint> {

	protected final boolean expectReply; // NOSONAR

	protected AmqpOutboundEndpointSpec(AmqpTemplate amqpTemplate, boolean expectReply) {
		this.expectReply = expectReply;
		this.target = new AmqpOutboundEndpoint(amqpTemplate);
		this.target.setExpectReply(expectReply);
		this.target.setHeaderMapper(this.headerMapper);
		if (expectReply) {
			this.target.setRequiresReply(true);
		}
	}

	@Override
	public S mappedReplyHeaders(String... headers) {
		Assert.isTrue(this.expectReply, "'mappedReplyHeaders' can be applied only for gateway");
		return super.mappedReplyHeaders(headers);
	}

	/**
	 * Wait for a publisher confirm.
	 * @param waitForConfirm true to wait.
	 * @return the spec.
	 * @since 5.2
	 * @see AmqpOutboundEndpoint#setWaitForConfirm(boolean)
	 */
	public S waitForConfirm(boolean waitForConfirm) {
		this.target.setWaitForConfirm(waitForConfirm);
		return _this();
	}

}
