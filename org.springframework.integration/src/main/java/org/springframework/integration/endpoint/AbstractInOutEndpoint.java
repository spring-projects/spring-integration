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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.integration.channel.ChannelRegistry;
import org.springframework.integration.message.CompositeMessage;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageBuilder;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.integration.message.MessageHeaders;
import org.springframework.integration.message.MessageRejectedException;
import org.springframework.integration.message.MessageTarget;
import org.springframework.integration.message.MessagingException;
import org.springframework.integration.message.selector.MessageSelector;

/**
 * @author Mark Fisher
 */
public abstract class AbstractInOutEndpoint extends AbstractEndpoint {

	private volatile MessageSelector selector;

	private volatile boolean requiresReply = false;

	private final List<EndpointInterceptor> interceptors = new CopyOnWriteArrayList<EndpointInterceptor>();


	public void setSelector(MessageSelector selector) {
		this.selector = selector;
	}

	public void setRequiresReply(boolean requiresReply) {
		this.requiresReply = requiresReply;
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

	@Override
	protected boolean sendInternal(Message<?> message) {
		for (EndpointInterceptor interceptor : this.interceptors) {
			message = interceptor.preHandle(message);
			if (message == null) {
				return false;
			}
		}
		if (!this.supports(message)) {
			throw new MessageRejectedException(message, "unsupported message");
		}
		Object result = this.handle(message);
		if (result == null) {
			if (this.requiresReply) {
				throw new MessageHandlingException(message, "endpoint '" + this.getName()
						+ " requires a reply, but no reply was received");
			}
			return true;
		}
		Message<?> reply = buildReplyMessage(result, message.getHeaders());
		MessageTarget replyTarget = this.resolveReplyTarget(message);
		if (reply instanceof CompositeMessage && this.shouldSplitComposite()) {
			boolean sentAtLeastOne = false;
			for (Message<?> nextReply : (CompositeMessage) reply) {
				boolean sent = this.sendReplyMessage(nextReply, replyTarget);
				sentAtLeastOne = (sentAtLeastOne || sent);
			}
			return sentAtLeastOne;
		}
		else {
			return this.sendReplyMessage(reply, replyTarget);
		}
	}

	protected abstract Object handle(Message<?> message);

	protected boolean supports(Message<?> message) {
		if (this.selector != null && !this.selector.accept(message)) {
			if (logger.isDebugEnabled()) {
				logger.debug("selector for endpoint '" + this + "' rejected message: " + message);
			}
			return false;
		}
		return true;
	}

	protected boolean shouldSplitComposite() {
		return false;
	}

	private boolean sendReplyMessage(Message<?> replyMessage, MessageTarget replyTarget) {
		for (int i = this.interceptors.size() - 1; i >= 0; i--) {
			EndpointInterceptor interceptor = this.interceptors.get(i);
			if (interceptor != null) {
				replyMessage = interceptor.postHandle(replyMessage);
				if (replyMessage == null) {
					return false;
				}
			}
		}
		return this.getMessageExchangeTemplate().send(replyMessage, replyTarget);
	}

	private Message<?> buildReplyMessage(Object result, MessageHeaders requestHeaders) {
		MessageBuilder<?> builder = null;
		if (result instanceof MessageBuilder) {
			builder = (MessageBuilder<?>) result;
		}
		else if (result instanceof CompositeMessage) {
			List<Message<?>> messages = ((CompositeMessage) result).getPayload();
			List<Message<?>> replies = new ArrayList<Message<?>>();
			for (Message<?> message : messages) {
				replies.add(this.buildReplyMessage(message, requestHeaders));
			}
			return new CompositeMessage(replies);
		}
		else if (result instanceof Message<?>) {
			builder = MessageBuilder.fromMessage((Message<?>) result);
		}
		else {
			builder = MessageBuilder.fromPayload(result);
		}
		return builder.copyHeadersIfAbsent(requestHeaders)
			.setHeaderIfAbsent(MessageHeaders.CORRELATION_ID, requestHeaders.getId())
			.build();
	}

	private MessageTarget resolveReplyTarget(Message<?> requestMessage) {
		MessageTarget replyTarget = this.getTarget();
		if (replyTarget == null) {
			Object returnAddress = requestMessage.getHeaders().getReturnAddress();
			if (returnAddress != null) {
				if (returnAddress instanceof MessageTarget) {
					replyTarget = (MessageTarget) returnAddress;
				}
				else if (returnAddress instanceof String) {
					ChannelRegistry channelRegistry = this.getChannelRegistry();
					if (channelRegistry != null) {
						replyTarget = channelRegistry.lookupChannel((String) returnAddress);
					}
				}
			}
		}
		if (replyTarget == null) {
			throw new MessagingException("unable to resolve reply target");
		}
		return replyTarget;
	}

}
