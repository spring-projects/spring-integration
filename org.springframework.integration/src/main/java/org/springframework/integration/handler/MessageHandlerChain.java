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

package org.springframework.integration.handler;

import java.util.List;

import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.message.MessageHandler;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.util.Assert;

/**
 * A composite {@link MessageHandler} implementation that invokes a chain of
 * MessageHandler instances in order.
 * 
 * @author Mark Fisher
 */
public class MessageHandlerChain extends IntegrationObjectSupport implements MessageHandler {

	private volatile List<MessageHandler> handlers;

	private volatile MessageChannel outputChannel;

	private volatile boolean initialized;

	private final Object initializationMonitor = new Object();


	public void setHandlers(List<MessageHandler> handlers) {
		this.handlers = handlers;
	}

	public void setOutputChannel(MessageChannel outputChannel) {
		this.outputChannel = outputChannel;
	}

	public final void afterPropertiesSet() {
		synchronized (this.initializationMonitor) {
			if (!this.initialized) {
				Assert.notEmpty(this.handlers, "handler list must not be empty");
				this.configureChain();
				this.initialized = true;
			}
		}
	}

	public void handleMessage(Message<?> message) {
		if (!this.initialized) {
			this.afterPropertiesSet();
		}
		this.handlers.get(0).handleMessage(message);
	}

	private void configureChain() {
		DirectChannel channel = null;
		List<MessageHandler> handlers = this.handlers;
		for (int i = 0; i < handlers.size(); i++) {
			boolean first = (i == 0);
			boolean last = (i == handlers.size() - 1);
			MessageHandler handler = handlers.get(i);
			if (!first) {
				EventDrivenConsumer consumer = new EventDrivenConsumer(channel, handler);
				consumer.start();
			}
			if (!last) {
				Assert.isInstanceOf(AbstractReplyProducingMessageHandler.class, handler,
						"All handlers except for the last one in the chain must implement "
						+ AbstractReplyProducingMessageHandler.class.getName());
				channel = new DirectChannel();
				channel.setBeanName("_" + this + ".channel#" + i);
				((AbstractReplyProducingMessageHandler) handler).setOutputChannel(channel);
			}
			else if (handler instanceof AbstractReplyProducingMessageHandler) {
				MessageChannel replyChannel = (this.outputChannel != null) ? this.outputChannel
						: new ReplyForwardingMessageChannel();
				((AbstractReplyProducingMessageHandler) handler).setOutputChannel(replyChannel);
			}
			else {
				Assert.isNull(this.outputChannel,
						"An output channel was provided, but the final handler in the chain is not an "
						+ "instance of [" + AbstractReplyProducingMessageHandler.class.getName() + "]");
			}
		}
	}


	private class ReplyForwardingMessageChannel implements MessageChannel {

		public String getName() {
			return MessageHandlerChain.this.getBeanName();
		}

		public boolean send(Message<?> message) {
			return this.send(message, -1);
		}

		public boolean send(Message<?> message, long timeout) {
			Object replyChannelHeader = message.getHeaders().getReplyChannel();
			if (replyChannelHeader == null) {
				throw new MessageHandlingException(message, "no replyChannel header available");
			}
			MessageChannel replyChannel = null;
			if (replyChannelHeader instanceof MessageChannel) {
				replyChannel = (MessageChannel) replyChannelHeader;
			}
			else if (replyChannelHeader instanceof String) {
				Assert.notNull(getChannelResolver(), "ChannelResolver is required");
				replyChannel = getChannelResolver().resolveChannelName((String) replyChannelHeader);
			}
			else {
				throw new MessageHandlingException(message, "invalid replyChannel type [" + replyChannelHeader.getClass() + "]");
			}
			return (timeout >= 0) ? replyChannel.send(message, timeout) : replyChannel.send(message);
		}
	}

}
