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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.integration.channel.ChannelRegistry;
import org.springframework.integration.channel.ChannelRegistryAware;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.message.CompositeMessage;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.message.MessageExchangeTemplate;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.integration.message.MessageHeaders;
import org.springframework.integration.message.MessageRejectedException;
import org.springframework.integration.message.MessageSource;
import org.springframework.integration.message.MessageTarget;
import org.springframework.integration.message.MessagingException;
import org.springframework.integration.message.selector.MessageSelector;
import org.springframework.integration.scheduling.Schedule;
import org.springframework.integration.util.ErrorHandler;
import org.springframework.util.Assert;

/**
 * The most basic Message Endpoint implementation. Serves as a "host" for any
 * {@link MessageHandler} and resolves the target for any reply Message(s)
 * returned by that handler. If the handler returns a non-empty
 * {@link CompositeMessage}, each Message in its list will be sent
 * as a separate reply Message.
 * 
 * <p>The reply target is resolved according to the following order:
 * <ol>
 *   <li>the 'nextTarget' header value of the reply Message</li>
 *   <li>the 'outputChannel' of this Message Endpoint</li>
 *   <li>the 'returnAddress' header value of the request Message</li>
 * </ol>
 * For the 'nextTarget' and 'returnAddress' values, either a
 * {@link MessageTarget} instance or String is accepted. If the
 * value is a String, then the endpoint will consult its
 * {@link ChannelRegistry} (typically provided by the MessageBus).
 * If no reply target can be determined for a non-null reply Message,
 * a {@link MessageEndpointReplyException} will be thrown.
 * 
 * @author Mark Fisher
 */
public class SimpleEndpoint<T extends MessageHandler> implements MessageEndpoint, ChannelRegistryAware, BeanNameAware {

	private final Log logger = LogFactory.getLog(this.getClass());

	private volatile String name;

	private final T handler;

	private volatile MessageChannel outputChannel;

	private volatile ErrorHandler errorHandler;

	private volatile ChannelRegistry channelRegistry;

	private volatile boolean requiresReply = false;

	private volatile MessageSelector selector;

	private final List<EndpointInterceptor> interceptors = new ArrayList<EndpointInterceptor>();

	private final MessageExchangeTemplate messageExchangeTemplate = new MessageExchangeTemplate();


	/**
	 * Create an endpoint for the given handler.
	 */
	public SimpleEndpoint(T handler) {
		Assert.notNull(handler, "handler must not be null");
		this.handler = handler;
	}


	public void setBeanName(String name) {
		this.name = name;
	}

	/**
	 * Return the name of this endpoint.
	 */
	public String getName() {
		return this.name;
	}

	/**
	 * Specify the channel where reply Messages should be sent if
	 * no 'nextTarget' header value is available on the reply Message.
	 */
	public void setOutputChannel(MessageChannel outputChannel) {
		this.outputChannel = outputChannel;
	}

	protected T getHandler() {
		return this.handler;
	}

	/**
	 * Provide an error handler for any Exceptions that occur
	 * upon invocation of this endpoint. If none is provided,
	 * the Exception messages will be logged (at warn level),
	 * and the Exception rethrown.
	 */
	public void setErrorHandler(ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
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

	public void setChannelRegistry(ChannelRegistry channelRegistry) {
		if (this.handler instanceof ChannelRegistryAware) {
			((ChannelRegistryAware) this.handler).setChannelRegistry(channelRegistry);
		}
		this.channelRegistry = channelRegistry;
	}

	protected ChannelRegistry getChannelRegistry() {
		return this.channelRegistry;
	}

	/**
	 * Specify whether this endpoint should throw an Exception when
	 * its handler returns an invalid reply Message. The reply Message
	 * is considered invalid if it is null, has a null payload, or has
	 * an empty Array or Collection as its payload. 
	 */
	public void setRequiresReply(boolean requiresReply) {
		this.requiresReply = requiresReply;
	}

	/**
	 * Specify the timeout for sending reply Messages to the reply
	 * target. The default value indicates an indefinite timeout. 
	 */
	public void setReplyTimeout(long replyTimeout) {
		this.messageExchangeTemplate.setSendTimeout(replyTimeout);
	}

	public final boolean send(Message<?> requestMessage) {
		if (requestMessage == null || requestMessage.getPayload() == null) {
			throw new IllegalArgumentException("Message and its payload must not be null");
		}
		if (this.logger.isDebugEnabled()) {
			this.logger.debug("endpoint '" + this + "' handling message: " + requestMessage);
		}
		try {
			Message<?> replyMessage = this.handleMessage(requestMessage, 0);
			if (!this.isValidReply(replyMessage)) {
				if (this.requiresReply) {
					throw new MessageHandlingException(requestMessage,
							"endpoint requires reply but none was received");
				}
			}
			else if (replyMessage instanceof CompositeMessage) {
				for (Message<?> nextReply : (CompositeMessage) replyMessage) {
					this.sendReplyMessage(nextReply, requestMessage);
				}
			}
			else {
				this.sendReplyMessage(replyMessage, requestMessage);
			}
			return true;
		}
		catch (Exception e) {
			if (e instanceof MessagingException) {
				this.handleException((MessagingException) e);
			}
			else {
				this.handleException(new MessageHandlingException(requestMessage,
						"failure occurred in endpoint's send operation", e));
			}
			return false;
		}
	}

	private Message<?> handleMessage(Message<?> requestMessage, final int index) {
		if (requestMessage == null || requestMessage.getPayload() == null) {
			return null;
		}
		if (index == 0) {
			for (EndpointInterceptor interceptor : this.interceptors) {
				requestMessage = interceptor.preHandle(requestMessage);
				if (requestMessage == null) {
					return null;
				}
			}
		}
		if (index == this.interceptors.size()) {
			if (!this.supports(requestMessage)) {
				throw new MessageRejectedException(requestMessage, "unsupported message");
			}
			Message<?> replyMessage = this.handler.handle(requestMessage);
			for (int i = index - 1; i >= 0; i--) {
				EndpointInterceptor interceptor = this.interceptors.get(i);
				replyMessage = interceptor.postHandle(requestMessage, replyMessage);
			}
			return replyMessage;
		}
		EndpointInterceptor nextInterceptor = this.interceptors.get(index);
		return nextInterceptor.aroundHandle(requestMessage, new MessageHandler() {
			@SuppressWarnings("unchecked")
			public Message<?> handle(Message message) {
				return SimpleEndpoint.this.handleMessage(message, index + 1);
			}
		});
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

	private void sendReplyMessage(Message<?> replyMessage, Message<?> requestMessage) {
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
				.setHeaderIfAbsent(MessageHeaders.CORRELATION_ID, requestMessage.getHeaders().getId()).build();
		if (!this.messageExchangeTemplate.send(replyMessage, replyTarget)) {
			throw new MessageEndpointReplyException(replyMessage, requestMessage,
					"failed to send reply to '" + replyTarget + "'");
		}
	}

	private void handleException(MessagingException exception) {
		if (this.errorHandler == null) {
			if (this.logger.isWarnEnabled()) {
				this.logger.warn("exception occurred in endpoint '" + this.name + "'", exception);
			}
			throw exception;
		}
		this.errorHandler.handle(exception);
	}

	private boolean isValidReply(Message<?> replyMessage) {
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

	private MessageTarget resolveReplyTarget(Message<?> replyMessage, MessageHeaders requestHeaders) {
		MessageTarget replyTarget = this.resolveTargetAttribute(replyMessage.getHeaders().getNextTarget());
		if (replyTarget == null) {
			replyTarget = this.outputChannel;
		}
		if (replyTarget == null) {
			replyTarget = this.resolveTargetAttribute(requestHeaders.getReturnAddress());
		}
		return replyTarget;
	}

	private MessageTarget resolveTargetAttribute(Object targetAttribute) {
		MessageTarget replyTarget = null;
		if (targetAttribute != null) {
			if (targetAttribute instanceof MessageTarget) {
				replyTarget = (MessageTarget) targetAttribute;
			}
			else if (targetAttribute instanceof String) {
				ChannelRegistry registry = getChannelRegistry();
				if (registry != null) {
					replyTarget = registry.lookupChannel((String) targetAttribute);
				}
			}
		}
		return replyTarget;
	}

	public String toString() {
		return (this.name != null) ? this.name : super.toString();
	}

	/* TODO: remove the following methods after they are removed from the MessageEndpoint interface. */

	private String inputChannelName;
	private String outputChannelName;
	private MessageSource<?> source;

	public String getInputChannelName() {
		return this.inputChannelName;
	}

	public void setInputChannelName(String inputChannelName) {
		this.inputChannelName = inputChannelName;
	}

	public String getOutputChannelName() {
		return this.outputChannelName;
	}

	public void setOutputChannelName(String outputChannelName) {
		this.outputChannelName = outputChannelName;
	}

	public void setReturnAddressOverrides(boolean b) {
	}

	public Schedule getSchedule() {
		return null;
	}

	public MessageSource<?> getSource() {
		return this.source;
	}

	public MessageTarget getTarget() {
		return this.outputChannel;
	}

	public void setSource(MessageSource<?> source) {
		this.source = source;
	}

	public void setTarget(MessageTarget target) {
		this.outputChannel = (MessageChannel) target;
	}

}
