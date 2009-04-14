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

import org.springframework.beans.BeanWrapperImpl;
import org.springframework.beans.PropertyAccessor;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.context.IntegrationObjectSupport;
import org.springframework.integration.core.Message;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.endpoint.EventDrivenConsumer;
import org.springframework.integration.filter.MessageFilter;
import org.springframework.integration.message.MessageHandler;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.util.Assert;

/**
 * A composite {@link MessageHandler} implementation that invokes a chain of
 * MessageHandler instances in order.
 * <p/>
 * Each of the handlers has to implement
 * <code>public void setOutputChannel(MessageChannel outputChannel);</code>. An
 * exception is made for the last handler in the case that the chain itself does
 * not have an output channel. No other assumptions about the type of handler
 * are made.
 * <p/>
 * It is expected that each handler will produce reply messages and send them to
 * its output channel, although this is not enforced. It is possible to filter
 * messages in the middle of the chain, for example using a
 * {@link MessageFilter}. A {@link MessageHandler} returning null will have the
 * same effect, although this option is less expressive.
 * <p/>
 * This component can be used from the namespace to improve the readability of
 * the configuration by removing channels that can be created implicitly.
 * <p/>
 * 
 * <pre>
 * &lt;chain&gt;
 *     &lt;filter ref=&quot;someFilter&quot;/&gt;
 *     &lt;bean class=&quot;SomeMessageHandlerImplementation&quot;/&gt;
 *     &lt;transformer ref=&quot;someTransformer&quot;/&gt;
 *     &lt;aggregator ... /&gt;
 * &lt;/chain&gt;
 * </pre>
 * 
 * @author Mark Fisher
 * @author Iwein Fuld
 */
public class MessageHandlerChain extends IntegrationObjectSupport implements
		MessageHandler {

	private static final String OUTPUT_CHANNEL_PROPERTY = "outputChannel";

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
				Assert
						.notEmpty(this.handlers,
								"handler list must not be empty");
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
			PropertyAccessor accessor = new BeanWrapperImpl(handler);
			if (!first) {
				EventDrivenConsumer consumer = new EventDrivenConsumer(channel,
						handler);
				consumer.start();
			}
			if (!last) {
				channel = new DirectChannel();
				channel.setBeanName("_" + this + ".channel#" + i);
				Assert.notNull(accessor
						.getPropertyType(OUTPUT_CHANNEL_PROPERTY),
						"All handlers except for the last one in the chain must implement property '"
								+ OUTPUT_CHANNEL_PROPERTY
								+ "' of type 'MessageChannel'");
				accessor.setPropertyValue(OUTPUT_CHANNEL_PROPERTY, channel);
			}
			else if (accessor.getPropertyType(OUTPUT_CHANNEL_PROPERTY) != null) {
				MessageChannel replyChannel = (this.outputChannel != null) ? this.outputChannel
						: new ReplyForwardingMessageChannel();
				accessor
						.setPropertyValue(OUTPUT_CHANNEL_PROPERTY, replyChannel);
			}
			else {
				Assert
						.isNull(
								this.outputChannel,
								"An output channel was provided, but the final handler in the chain does not implement property '"
										+ OUTPUT_CHANNEL_PROPERTY
										+ "'  of type 'MessageChannel'");
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
				throw new MessageHandlingException(message,
						"no replyChannel header available");
			}
			MessageChannel replyChannel = null;
			if (replyChannelHeader instanceof MessageChannel) {
				replyChannel = (MessageChannel) replyChannelHeader;
			}
			else if (replyChannelHeader instanceof String) {
				Assert.notNull(getChannelResolver(),
						"ChannelResolver is required");
				replyChannel = getChannelResolver().resolveChannelName(
						(String) replyChannelHeader);
			}
			else {
				throw new MessageHandlingException(message,
						"invalid replyChannel type ["
								+ replyChannelHeader.getClass() + "]");
			}
			return (timeout >= 0) ? replyChannel.send(message, timeout)
					: replyChannel.send(message);
		}
	}
}
