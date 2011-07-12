/*
 * Copyright 2002-2011 the original author or authors.
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

package org.springframework.integration.gateway;

import java.io.Serializable;

import org.springframework.integration.Message;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.handler.AbstractReplyProducingMessageHandler;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.remoting.RemoteAccessException;

/**
 * A base class for outbound URL-based Messaging Gateways.
 * 
 * @author Mark Fisher
 * 
 * @deprecated as of 2.0. Will be removed in 2.1.
 */
@Deprecated
public abstract class AbstractRemotingOutboundGateway extends AbstractReplyProducingMessageHandler {

	private final RequestReplyExchanger proxy;


	public AbstractRemotingOutboundGateway(String url) {
		this.proxy = this.createProxy(url);
	}


	public void setReplyChannel(MessageChannel replyChannel) {
		this.setOutputChannel(replyChannel);
	}

	/**
	 * Subclasses must implement this method. It will be invoked from the constructor.
	 */
	protected abstract RequestReplyExchanger createProxy(String url);


	@Override
	public final Object handleRequestMessage(Message<?> message) {
		if (!(message.getPayload() instanceof Serializable)) {
			throw new MessageHandlingException(message,
					this.getClass().getName() + " expects a Serializable payload type " +
					"but encountered [" + message.getPayload().getClass().getName() + "]");
		}
		Message<?> requestMessage = MessageBuilder.withPayload(message.getPayload())
				.copyHeaders(message.getHeaders()).build();
		try {
			Message<?> reply = this.proxy.exchange(requestMessage);
			if (reply != null) {
				reply = MessageBuilder.fromMessage(reply).copyHeadersIfAbsent(message.getHeaders()).build();
			}
			return reply;
		}
		catch (RemoteAccessException e) {
			throw new MessageHandlingException(message, "remote failure in Messaging Gateway", e);
		}
	}

}
