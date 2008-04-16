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

import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.integration.channel.ChannelRegistry;
import org.springframework.integration.channel.ChannelRegistryAware;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.handler.MessageHandlerNotRunningException;
import org.springframework.integration.handler.MessageHandlerRejectedExecutionException;
import org.springframework.integration.handler.ReplyHandler;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageDeliveryException;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.integration.message.MessageHeader;
import org.springframework.integration.message.selector.MessageSelector;
import org.springframework.integration.message.selector.MessageSelectorRejectedException;
import org.springframework.integration.scheduling.Subscription;
import org.springframework.integration.util.ErrorHandler;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Default implementation of the {@link MessageEndpoint} interface.
 * 
 * @author Mark Fisher
 */
public class DefaultMessageEndpoint implements MessageEndpoint, ChannelRegistryAware, InitializingBean, BeanNameAware {

	private final Log logger = LogFactory.getLog(this.getClass());

	private volatile String name;

	private volatile MessageHandler handler;

	private volatile Subscription subscription;

	private volatile ConcurrencyPolicy concurrencyPolicy;

	private volatile ErrorHandler errorHandler;

	private final List<MessageSelector> selectors = new CopyOnWriteArrayList<MessageSelector>();

	private volatile ReplyHandler replyHandler = new EndpointReplyHandler();

	private volatile long replyTimeout = 1000;

	private volatile String defaultOutputChannelName;

	private volatile ChannelRegistry channelRegistry;

	private volatile boolean initialized;

	private volatile boolean running;


	public DefaultMessageEndpoint(MessageHandler handler) {
		Assert.notNull(handler, "handler must not be null");
		this.handler = handler;
	}


	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setBeanName(String beanName) {
		this.setName(beanName);
	}

	public MessageHandler getHandler() {
		return this.handler;
	}

	/**
	 * Set the handler to be invoked for each consumed message.
	 */
	public void setHandler(MessageHandler handler) {
		this.handler = handler;
	}

	public void setMessageSelectors(List<MessageSelector> selectors) {
		this.selectors.clear();
		this.selectors.addAll(selectors);
	}

	public void addMessageSelector(MessageSelector messageSelector) {
		Assert.notNull(messageSelector, "'messageSelector' must not be null");
		this.selectors.add(messageSelector);
	}

	public Subscription getSubscription() {
		return this.subscription;
	}

	public void setSubscription(Subscription subscription) {
		this.subscription = subscription;
	}

	public ConcurrencyPolicy getConcurrencyPolicy() {
		return this.concurrencyPolicy;
	}

	public void setConcurrencyPolicy(ConcurrencyPolicy concurrencyPolicy) {
		this.concurrencyPolicy = concurrencyPolicy;
	}

	public void setErrorHandler(ErrorHandler errorHandler) {
		this.errorHandler = errorHandler;
	}

	public boolean hasErrorHandler() {
		return (this.errorHandler != null);
	}

	public void setReplyHandler(ReplyHandler replyHandler) {
		Assert.notNull(replyHandler, "'replyHandler' must not be null");
		this.replyHandler = replyHandler;
	}

	/**
	 * Set the timeout in milliseconds to be enforced when this endpoint sends a
	 * reply message. If the message is not sent successfully within the
	 * allotted time, then it will be sent within a MessageDeliveryException to
	 * the error handler instead. The default <code>replyTimeout</code> value
	 * is 1000 milliseconds.
	 */
	public void setReplyTimeout(long replyTimeout) {
		this.replyTimeout = replyTimeout;
	}

	public String getDefaultOutputChannelName() {
		return this.defaultOutputChannelName;
	}

	/**
	 * Set the name of the channel to which this endpoint should send reply
	 * messages by default.
	 */
	public void setDefaultOutputChannelName(String defaultOutputChannelName) {
		this.defaultOutputChannelName = defaultOutputChannelName;
	}

	/**
	 * Set the channel registry to use for looking up channels by name.
	 */
	public void setChannelRegistry(ChannelRegistry channelRegistry) {
		this.channelRegistry = channelRegistry;
	}

	public void afterPropertiesSet() {
		if (this.handler instanceof ChannelRegistryAware) {
			((ChannelRegistryAware) this.handler).setChannelRegistry(this.channelRegistry);
		}
		if (this.concurrencyPolicy != null && !(this.handler instanceof ConcurrentHandler)) {
			int capacity = this.concurrencyPolicy.getQueueCapacity();
			BlockingQueue<Runnable> queue = (capacity < 1) ? new SynchronousQueue<Runnable>() : new ArrayBlockingQueue<Runnable>(capacity);
			ExecutorService executor = new ThreadPoolExecutor(
					this.concurrencyPolicy.getCoreSize(), this.concurrencyPolicy.getMaxSize(),
					this.concurrencyPolicy.getKeepAliveSeconds(), TimeUnit.SECONDS, queue);
			this.handler = new ConcurrentHandler(this.handler, executor);
		}
		if (this.handler instanceof ConcurrentHandler) {
			if (this.errorHandler != null) {
				((ConcurrentHandler) this.handler).setErrorHandler(this.errorHandler);
			}
			((ConcurrentHandler) this.handler).setReplyHandler(this.replyHandler);
		}
		this.initialized = true;
	}

	public boolean isRunning() {
		return this.running;
	}

	public void start() {
		if (this.isRunning()) {
			return;
		}
		if (!initialized) {
			this.afterPropertiesSet();
		}
		this.running = true;
	}

	public void stop() {
		if (!this.isRunning()) {
			return;
		}
		this.running = false;
	}

	public final Message<?> handle(Message<?> message) {
		if (logger.isDebugEnabled()) {
			logger.debug("endpoint '" + this + "' handling message: " + message);
		}
		if (!this.isRunning()) {
			throw new MessageHandlerNotRunningException(message);
		}
		for (MessageSelector selector : this.selectors) {
			if (!selector.accept(message)) {
				throw new MessageSelectorRejectedException(message);
			}
		}
		try {
			Message<?> replyMessage = this.handler.handle(message);
			if (replyMessage != null) {
				if (replyMessage.getHeader().getCorrelationId() == null) {
					replyMessage.getHeader().setCorrelationId(message.getId());
				}
				this.replyHandler.handle(replyMessage, message.getHeader());
			}
		}
		catch (MessageHandlerRejectedExecutionException e) {
			throw e;
		}
		catch (Throwable t) {
			if (this.errorHandler == null) {
				throw new MessageHandlingException(message,
						"error occurred in endpoint, and no 'errorHandler' available", t);
			}
			this.errorHandler.handle(t);
		}
		return null;
	}

	public String toString() {
		return (this.name != null) ? this.name : super.toString();
	}

	private MessageChannel resolveReplyChannel(MessageHeader originalMessageHeader) {
		Object returnAddress = originalMessageHeader.getReturnAddress();
		if (returnAddress instanceof MessageChannel) {
			return (MessageChannel) returnAddress;
		}
		if (returnAddress instanceof String && this.channelRegistry != null) {
			String channelName = (String) returnAddress;
			if (StringUtils.hasText(channelName)) {
				return this.channelRegistry.lookupChannel(channelName);
			}
		}
		if (this.defaultOutputChannelName != null && this.channelRegistry != null) {
			return this.channelRegistry.lookupChannel(this.defaultOutputChannelName);
		}
		return null;
	}


	private class EndpointReplyHandler implements ReplyHandler {

		public void handle(Message<?> replyMessage, MessageHeader originalMessageHeader) {
			if (replyMessage == null) {
				return;
			}
			MessageChannel replyChannel = resolveReplyChannel(originalMessageHeader);
			if (replyChannel == null) {
				throw new MessageHandlingException(replyMessage, 
						"Unable to determine reply channel for message. Provide a 'returnAddress' in the message header " +
						"or a 'defaultOutputChannelName' on the message endpoint.");
			}
			if (logger.isDebugEnabled()) {
				logger.debug("endpoint '" + DefaultMessageEndpoint.this + "' replying to channel '" + replyChannel + "' with message: " + replyMessage);
			}
			if (!replyChannel.send(replyMessage, replyTimeout)) {
				throw new MessageDeliveryException(replyMessage,
						"unable to send reply message within alloted timeout of " + replyTimeout + " milliseconds");
			}
		}
	}

}
