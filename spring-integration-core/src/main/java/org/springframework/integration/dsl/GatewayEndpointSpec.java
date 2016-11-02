/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.integration.dsl;

import org.springframework.messaging.MessageChannel;

/**
 * A {@link ConsumerEndpointSpec} implementation for a mid-flow {@link GatewayMessageHandler}.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
public final class GatewayEndpointSpec extends ConsumerEndpointSpec<GatewayEndpointSpec, GatewayMessageHandler> {

	GatewayEndpointSpec(MessageChannel requestChannel) {
		super(new GatewayMessageHandler());
		this.handler.setRequestChannel(requestChannel);
	}

	GatewayEndpointSpec(String requestChannel) {
		super(new GatewayMessageHandler());
		this.handler.setRequestChannelName(requestChannel);
	}

	public GatewayEndpointSpec replyChannel(MessageChannel replyChannel) {
		this.handler.setReplyChannel(replyChannel);
		return this;
	}

	public GatewayEndpointSpec replyChannel(String replyChannel) {
		this.handler.setReplyChannelName(replyChannel);
		return this;
	}

	public GatewayEndpointSpec errorChannel(MessageChannel errorChannel) {
		this.handler.setErrorChannel(errorChannel);
		return this;
	}

	public GatewayEndpointSpec errorChannel(String errorChannel) {
		this.handler.setErrorChannelName(errorChannel);
		return this;
	}

	public GatewayEndpointSpec requestTimeout(Long requestTimeout) {
		this.handler.setRequestTimeout(requestTimeout);
		return this;
	}

	public GatewayEndpointSpec replyTimeout(Long replyTimeout) {
		this.handler.setReplyTimeout(replyTimeout);
		return this;
	}

}
