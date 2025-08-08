/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.dsl;

import org.springframework.integration.gateway.GatewayMessageHandler;
import org.springframework.messaging.MessageChannel;

/**
 * A {@link ConsumerEndpointSpec} implementation for a mid-flow {@link GatewayMessageHandler}.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
public class GatewayEndpointSpec extends ConsumerEndpointSpec<GatewayEndpointSpec, GatewayMessageHandler> {

	protected GatewayEndpointSpec(MessageChannel requestChannel) {
		super(new GatewayMessageHandler());
		this.handler.setRequestChannel(requestChannel);
	}

	protected GatewayEndpointSpec(String requestChannel) {
		super(new GatewayMessageHandler());
		this.handler.setRequestChannelName(requestChannel);
	}

	/**
	 * Set a reply channel.
	 * @param replyChannel the reply channel
	 * @return the spec
	 */
	public GatewayEndpointSpec replyChannel(MessageChannel replyChannel) {
		this.handler.setReplyChannel(replyChannel);
		return this;
	}

	/**
	 * Set a reply channel.
	 * @param replyChannel the reply channel
	 * @return the spec
	 */
	public GatewayEndpointSpec replyChannel(String replyChannel) {
		this.handler.setReplyChannelName(replyChannel);
		return this;
	}

	/**
	 * Set an error channel.
	 * @param errorChannel the error channel
	 * @return the spec
	 */
	public GatewayEndpointSpec errorChannel(MessageChannel errorChannel) {
		this.handler.setErrorChannel(errorChannel);
		return this;
	}

	/**
	 * Set an error channel.
	 * @param errorChannel the error channel
	 * @return the spec
	 */
	public GatewayEndpointSpec errorChannel(String errorChannel) {
		this.handler.setErrorChannelName(errorChannel);
		return this;
	}

	/**
	 * Set a request timeout.
	 * @param requestTimeout the request timeout
	 * @return the spec
	 */
	public GatewayEndpointSpec requestTimeout(Long requestTimeout) {
		this.handler.setRequestTimeout(requestTimeout);
		return this;
	}

	/**
	 * Set a reply timeout.
	 * @param replyTimeout the reply timeout
	 * @return the spec
	 */
	public GatewayEndpointSpec replyTimeout(Long replyTimeout) {
		this.handler.setReplyTimeout(replyTimeout);
		return this;
	}

	/**
	 * Set a error on timeout flag.
	 * @param errorOnTimeout true to produce an error in case of a reply timeout.
	 * @return the spec.
	 * @since 6.2
	 * @see org.springframework.integration.gateway.GatewayProxyFactoryBean#setErrorOnTimeout(boolean)
	 */
	public GatewayEndpointSpec errorOnTimeout(boolean errorOnTimeout) {
		this.handler.setErrorOnTimeout(errorOnTimeout);
		return this;
	}

}
