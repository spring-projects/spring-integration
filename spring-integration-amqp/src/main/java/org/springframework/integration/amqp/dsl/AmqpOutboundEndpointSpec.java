/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
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
