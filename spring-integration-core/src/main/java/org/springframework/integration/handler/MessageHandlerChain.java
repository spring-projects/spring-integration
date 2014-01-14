/*
 * Copyright 2002-2014 the original author or authors.
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

import java.util.HashSet;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;

import org.springframework.beans.DirectFieldAccessor;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.context.Lifecycle;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.filter.MessageFilter;
import org.springframework.integration.support.channel.BeanFactoryChannelResolver;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.core.DestinationResolver;
import org.springframework.util.Assert;

/**
 * A composite {@link MessageHandler} implementation that invokes a chain of
 * MessageHandler instances in order.
 * <p>
 * Each of the handlers except for the last one <b>must</b> implement the
 * {@link MessageProducer} interface. The last handler must also <b>if</b>
 * the chain itself has an output channel configured. No other assumptions
 * are made about the type of handler.
 * <p>
 * It is expected that each handler will produce reply messages and send them to
 * its output channel, although this is not enforced. It is possible to filter
 * messages in the middle of the chain, for example using a
 * {@link MessageFilter}. A {@link MessageHandler} returning null will have the
 * same effect, although this option is less expressive.
 * <p>
 * This component can be used from the namespace to improve the readability of
 * the configuration by removing channels that can be created implicitly.
 *
 * <pre class="code">
 * {@code
 * <chain>
 *     <filter ref="someFilter"/>
 *     <bean class="SomeMessageHandlerImplementation"/>
 *     <transformer ref="someTransformer"/>
 *     <aggregator ... />
 * </chain>
 * }
 * </pre>
 *
 * @author Mark Fisher
 * @author Iwein Fuld
 * @author Gary Russell
 * @author Artem Bilan
 */
public class MessageHandlerChain extends AbstractMessageHandler implements MessageProducer, Lifecycle {

	private volatile List<MessageHandler> handlers;

	private volatile MessageChannel outputChannel;

	/**
	 * If the sendTimeout is configured explicitly on this chain instance, it will
	 * take precedence over the actual settings on the final handler in the chain.
	 * By default, it is <code>null</code>, so the actual handler configuration is used.
	 */
	private volatile Long sendTimeout = null;

	private volatile DestinationResolver<MessageChannel> channelResolver;

	private volatile boolean initialized;

	private final Object initializationMonitor = new Object();

	private volatile boolean running;

	private final ReentrantLock lifecycleLock = new ReentrantLock();

	public void setHandlers(List<MessageHandler> handlers) {
		this.handlers = handlers;
	}

	@Override
	public void setOutputChannel(MessageChannel outputChannel) {
		this.outputChannel = outputChannel;
	}

	public void setSendTimeout(long sendTimeout) {
		this.sendTimeout = sendTimeout;
	}

	@Override
	public String getComponentType() {
		return "chain";
	}

	@Override
	protected void onInit() throws Exception {
		synchronized (this.initializationMonitor) {
			if (!this.initialized) {
				Assert.notEmpty(this.handlers, "handler list must not be empty");
				this.configureChain();
				BeanFactory beanFactory = this.getBeanFactory();
				if (this.channelResolver == null && beanFactory != null) {
					this.channelResolver = new BeanFactoryChannelResolver(beanFactory);
				}
				this.initialized = true;
			}
		}
	}

	@Override
	protected void handleMessageInternal(Message<?> message) throws Exception {
		if (!this.initialized) {
			this.onInit();
		}
		this.handlers.get(0).handleMessage(message);
	}

	private void configureChain() {
		Assert.isTrue(this.handlers.size() == new HashSet<MessageHandler>(this.handlers).size(),
				"duplicate handlers are not allowed in a chain");
		for (int i = 0; i < this.handlers.size(); i++) {
			MessageHandler handler = handlers.get(i);
			if (i < handlers.size() - 1) { // not the last handler
				Assert.isTrue(handler instanceof MessageProducer, "All handlers except for " +
						"the last one in the chain must implement the MessageProducer interface.");
				final MessageHandler nextHandler = handlers.get(i + 1);
				final MessageChannel nextChannel = new MessageChannel() {
					@Override
					public boolean send(Message<?> message, long timeout) {
						return this.send(message);
					}
					@Override
					public boolean send(Message<?> message) {
						nextHandler.handleMessage(message);
						return true;
					}
				};
				((MessageProducer) handler).setOutputChannel(nextChannel);

				// If this 'handler' is a nested non-last &lt;chain&gt;, it is  necessary
				// to 'force' re-init it for check its configuration in conjunction with current MessageHandlerChain.
				if (handler instanceof MessageHandlerChain) {
					new DirectFieldAccessor(handler).setPropertyValue("initialized", false);
					((MessageHandlerChain) handler).afterPropertiesSet();
				}
			}
			else if (handler instanceof MessageProducer) {
				MessageChannel replyChannel = new ReplyForwardingMessageChannel();
				((MessageProducer) handler).setOutputChannel(replyChannel);
			}
			else {
				Assert.isNull(this.outputChannel,
						"An output channel was provided, but the final handler in " +
						"the chain does not implement the MessageProducer interface.");
			}
		}
	}

	/**
	 * SmartLifecycle implementation (delegates to the {@link #handlers})
	 */

	@Override
	public final boolean isRunning() {
		this.lifecycleLock.lock();
		try {
			return this.running;
		}
		finally {
			this.lifecycleLock.unlock();
		}
	}

	@Override
	public final void start() {
		this.lifecycleLock.lock();
		try {
			if (!this.running) {
				this.doStart();
				this.running = true;
				if (logger.isInfoEnabled()) {
					logger.info("started " + this);
				}
			}
		}
		finally {
			this.lifecycleLock.unlock();
		}
	}

	@Override
	public final void stop() {
		this.lifecycleLock.lock();
		try {
			if (this.running) {
				this.doStop();
				this.running = false;
				if (logger.isInfoEnabled()) {
					logger.info("stopped " + this);
				}
			}
		}
		finally {
			this.lifecycleLock.unlock();
		}
	}

	public final void stop(Runnable callback) {
		this.lifecycleLock.lock();
		try {
			this.stop();
			callback.run();
		}
		finally {
			this.lifecycleLock.unlock();
		}
	}

	private void doStop() {
		for (MessageHandler handler : this.handlers) {
			if (handler instanceof Lifecycle) {
				((Lifecycle) handler).stop();
			}
		}
	}

	private void doStart() {
		for (MessageHandler handler : this.handlers) {
			if (handler instanceof Lifecycle) {
				((Lifecycle) handler).start();
			}
		}
	}

	private class ReplyForwardingMessageChannel implements MessageChannel {

		@Override
		public boolean send(Message<?> message) {
			return this.send(message, -1);
		}

		@Override
		public boolean send(Message<?> message, long timeout) {
			timeout = (MessageHandlerChain.this.sendTimeout != null)
					? MessageHandlerChain.this.sendTimeout : timeout;
			if (MessageHandlerChain.this.outputChannel != null) {
				return MessageHandlerChain.this.outputChannel.send(message, timeout);
			}
			Object replyChannelHeader = message.getHeaders().getReplyChannel();
			if (replyChannelHeader == null) {
				throw new MessageHandlingException(message, "no replyChannel header available");
			}
			MessageChannel replyChannel = null;
			if (replyChannelHeader instanceof MessageChannel) {
				replyChannel = (MessageChannel) replyChannelHeader;
			}
			else if (replyChannelHeader instanceof String) {
				Assert.notNull(channelResolver, "ChannelResolver is required");
				replyChannel = channelResolver.resolveDestination((String) replyChannelHeader);
			}
			else {
				throw new MessageHandlingException(message,
						"invalid replyChannel type [" + replyChannelHeader.getClass() + "]");
			}
			return (timeout >= 0) ? replyChannel.send(message, timeout)
					: replyChannel.send(message);
		}
	}

}
