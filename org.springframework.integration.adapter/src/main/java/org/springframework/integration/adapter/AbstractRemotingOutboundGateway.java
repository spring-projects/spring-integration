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

import java.io.Serializable;

import org.springframework.integration.consumer.AbstractReplyProducingMessageHandler;
import org.springframework.integration.consumer.ReplyMessageHolder;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.remoting.RemoteAccessException;

/**
 * A base class for outbound Messaging Gateways that use url-based remoting.
 * 
 * @author Mark Fisher
 */
public abstract class AbstractRemotingOutboundGateway extends AbstractReplyProducingMessageHandler {

	private final RemoteMessageHandler handlerProxy;


	public AbstractRemotingOutboundGateway(String url) {
		this.handlerProxy = this.createHandlerProxy(url);
	}


	public void setReplyChannel(MessageChannel replyChannel) {
		this.setOutputChannel(replyChannel);
	}

	/**
	 * Subclasses must implement this method. It will be invoked from the constructor.
	 */
	protected abstract RemoteMessageHandler createHandlerProxy(String url);


	@Override
	public final void handleRequestMessage(Message<?> message, ReplyMessageHolder replyHolder) {
		if (!(message.getPayload() instanceof Serializable)) {
			throw new MessageHandlingException(message,
					this.getClass().getName() + " expects a Serializable payload type " +
					"but encountered [" + message.getPayload().getClass().getName() + "]");
		}
		Message<?> requestMessage = MessageBuilder.fromMessage(message).build();
		try {
			Message<?> reply = this.handlerProxy.handle(requestMessage);
			if (reply != null) {
				replyHolder.set(reply);
			}
		}
		catch (RemoteAccessException e) {
			throw new MessageHandlingException(message, "unable to handle message remotely", e);
		}
	}

}
