/*
 * Copyright 2016-2020 the original author or authors.
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

}
