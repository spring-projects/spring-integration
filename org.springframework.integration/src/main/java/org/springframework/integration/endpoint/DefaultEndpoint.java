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

package org.springframework.integration.endpoint;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.springframework.integration.channel.ChannelRegistry;
import org.springframework.integration.channel.ChannelRegistryAware;
import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.message.CompositeMessage;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.integration.message.MessageHeaders;
import org.springframework.integration.message.MessageRejectedException;
import org.springframework.integration.message.MessageTarget;
import org.springframework.integration.message.selector.MessageSelector;
import org.springframework.util.Assert;

/**
 * The default Message Endpoint implementation. Serves as a "host" for any
 * {@link MessageHandler} and resolves the target for any reply Message(s)
 * returned by that handler. If the handler returns a non-empty
 * {@link CompositeMessage}, each Message in its list will be sent
 * as a separate reply Message.
 * 
 * <p>The reply target is resolved according to the following order:
 * <ol>
 *   <li>the 'outputChannel' of this Message Endpoint</li>
 *   <li>the 'returnAddress' header value of the request Message</li>
 * </ol>
 * For the 'returnAddress' value, either a {@link MessageTarget} instance
 * or String is accepted. If the value is a String, then the endpoint will
 * consult its {@link ChannelRegistry} (typically provided by the MessageBus).
 * If no reply target can be determined for a non-null reply Message,
 * a {@link MessageEndpointReplyException} will be thrown.
 * 
 * @author Mark Fisher
 */
public class DefaultEndpoint<T extends MessageHandler> extends AbstractRequestReplyEndpoint {

	private final T handler;

	private volatile MessageSelector selector;

	private final List<EndpointInterceptor> interceptors = new ArrayList<EndpointInterceptor>();


	/**
	 * Create an endpoint for the given handler.
	 */
	public DefaultEndpoint(T handler) {
		Assert.notNull(handler, "handler must not be null");
		this.handler = handler;
	}


	protected T getHandler() {
		return this.handler;
	}

	@Override
	public void setChannelRegistry(ChannelRegistry channelRegistry) {
		super.setChannelRegistry(channelRegistry);
		if (this.handler instanceof ChannelRegistryAware) {
			((ChannelRegistryAware) this.handler).setChannelRegistry(channelRegistry);
		}
	}

	public void setSelector(MessageSelector selector) {
		this.selector = selector;
	}

	public void addInterceptor(EndpointInterceptor interceptor) {
		this.interceptors.add(interceptor);
	}

	public void setInterceptors(List<EndpointInterceptor> interceptors) {
		this.interceptors.clear();
		for (EndpointInterceptor interceptor : interceptors) {
			this.addInterceptor(interceptor);
		}
	}

	/**
	 * Specify the timeout for sending reply Messages to the reply
	 * target. The default value indicates an indefinite timeout. 
	 */
	public void setReplyTimeout(long replyTimeout) {
		this.getMessageExchangeTemplate().setSendTimeout(replyTimeout);
	}

	@Override
	public final Message<?> handleRequestMessage(Message<?> requestMessage) {
		if (requestMessage == null || requestMessage.getPayload() == null) {
			return null;
		}
		for (EndpointInterceptor interceptor : this.interceptors) {
			requestMessage = interceptor.preHandle(requestMessage);
			if (requestMessage == null) {
				return null;
			}
		}
		if (!this.supports(requestMessage)) {
			throw new MessageRejectedException(requestMessage, "unsupported message");
		}
		Message<?> replyMessage = this.handler.handle(requestMessage);
		for (int i = this.interceptors.size() - 1; i >= 0; i--) {
			EndpointInterceptor interceptor = this.interceptors.get(i);
			if (interceptor != null) {
				replyMessage = interceptor.postHandle(replyMessage);
			}
		}
		return replyMessage;
	}

	/**
	 * The reply Message is considered invalid if it is null, has a null payload,
	 * or has an empty Array or Collection as its payload.
	 */
	@Override
	protected boolean isValidReplyMessage(Message<?> replyMessage) {
		if (replyMessage == null) {
			return false;
		}
		Object payload = replyMessage.getPayload();
		if (payload == null) {
			return false;
		}
		if (payload.getClass().isArray() && Array.getLength(payload) == 0) {
			return false;
		}
		if (payload instanceof Collection && ((Collection<?>) payload).size() == 0) {
			return false;
		}
		return true;
	}

	@Override
	protected void sendReplyMessage(Message<?> replyMessage, Message<?> requestMessage) {
		if (replyMessage == null) {
			throw new MessageHandlingException(requestMessage, "reply message must not be null");
		}
		MessageTarget replyTarget = this.resolveReplyTarget(replyMessage, requestMessage.getHeaders());
		if (replyTarget == null) {
			throw new MessageEndpointReplyException(replyMessage, requestMessage,
					"unable to resolve reply target");
		}
		replyMessage = MessageBuilder.fromMessage(replyMessage)
				.copyHeadersIfAbsent(requestMessage.getHeaders())
				.setHeaderIfAbsent(MessageHeaders.CORRELATION_ID, requestMessage.getHeaders().getId())
                .build();
		if (!this.getMessageExchangeTemplate().send(replyMessage, replyTarget)) {
			throw new MessageEndpointReplyException(replyMessage, requestMessage,
					"failed to send reply to '" + replyTarget + "'");
		}
	}

	protected boolean supports(Message<?> message) {
		if (this.selector != null && !this.selector.accept(message)) {
			if (logger.isDebugEnabled()) {
				logger.debug("selector for endpoint '" + this + "' rejected message: " + message);
			}
			return false;
		}
		return true;
	}

	private MessageTarget resolveReplyTarget(Message<?> replyMessage, MessageHeaders requestHeaders) {
		MessageTarget replyTarget = this.getTarget();
		if (replyTarget == null) {
			Object returnAddress = requestHeaders.getReturnAddress();
			if (returnAddress != null) {
				if (returnAddress instanceof MessageTarget) {
					replyTarget = (MessageTarget) returnAddress;
				}
				else if (returnAddress instanceof String) {
					ChannelRegistry registry = this.getChannelRegistry();
					if (registry != null) {
						replyTarget = registry.lookupChannel((String) returnAddress);
					}
				}
			}
		}
		return replyTarget;
	}

}
