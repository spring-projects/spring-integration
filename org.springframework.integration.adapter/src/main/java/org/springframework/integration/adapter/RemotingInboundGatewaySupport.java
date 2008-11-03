/*
 * Copyright 2002-2008 the original author or authors.
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

package org.springframework.integration.adapter;

import org.springframework.integration.core.Message;
import org.springframework.integration.gateway.SimpleMessagingGateway;

/**
 * Support class for inbound Messaging Gateways.
 * 
 * @author Mark Fisher
 */
public abstract class RemotingInboundGatewaySupport extends SimpleMessagingGateway implements RemoteMessageHandler {

	private volatile boolean expectReply = true;


	/**
	 * Specify whether the gateway should be expected to return a reply.
	 * The default is '<code>true</code>'.
	 */
	public void setExpectReply(boolean expectReply) {
		this.expectReply = expectReply;
	}

	public Message<?> handle(Message<?> message) {
		if (this.expectReply) {
			return this.sendAndReceiveMessage(message);
		}
		this.send(message);
		return null;
	}

}
