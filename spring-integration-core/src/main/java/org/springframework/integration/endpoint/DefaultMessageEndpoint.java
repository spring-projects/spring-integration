/*
 * Copyright 2002-2007 the original author or authors.
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

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.Lifecycle;
import org.springframework.integration.MessageHandlingException;
import org.springframework.integration.MessagingConfigurationException;
import org.springframework.integration.channel.ChannelRegistry;
import org.springframework.integration.channel.ChannelRegistryAware;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.handler.ConcurrentHandler;
import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.handler.MessageHandlerNotRunningException;
import org.springframework.integration.handler.MessageHandlerRejectedExecutionException;
import org.springframework.integration.handler.ReplyHandler;
import org.springframework.integration.message.Message;
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

	private String name;

	private MessageHandler handler;

	private List<MessageSelector> selectors = new CopyOnWriteArrayList<MessageSelector>();

	private Subscription subscription;

	private ConcurrencyPolicy concurrencyPolicy;

	private ErrorHandler errorHandler;

	private ReplyHandler replyHandler = new EndpointReplyHandler();

	private String defaultOutputChannelName;

	private ChannelRegistry channelRegistry;

	private volatile boolean initialized;

	private volatile boolean running;


	public DefaultMessageEndpoint() {
	}

	public DefaultMessageEndpoint(MessageHandler handler) {
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
		this.selectors = new CopyOnWriteArrayList<MessageSelector>(selectors);
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

	public String getDefaultOutputChannelName() {
		return this.defaultOutputChannelName;
	}

	/**
	 * Set the name of the channel to which this endpoint can send reply messages by default.
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
		if (this.concurrencyPolicy != null || this.handler instanceof ConcurrentHandler) {
			if (!(this.handler instanceof ConcurrentHandler)) {
				int capacity = concurrencyPolicy.getQueueCapacity();
				BlockingQueue<Runnable> queue = (capacity < 1) ? new SynchronousQueue<Runnable>() :
						new ArrayBlockingQueue<Runnable>(capacity);
				ExecutorService executor = new ThreadPoolExecutor(concurrencyPolicy.getCoreSize(),
						concurrencyPolicy.getMaxSize(), concurrencyPolicy.getKeepAliveSeconds(),
						TimeUnit.SECONDS, queue);
				this.handler = new ConcurrentHandler(this.handler, executor);
			}
			ConcurrentHandler concurrentHandler = (ConcurrentHandler) this.handler;
			if (this.errorHandler != null) {
				concurrentHandler.setErrorHandler(this.errorHandler);
			}
			concurrentHandler.setReplyHandler(this.replyHandler);
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
		if (this.handler instanceof Lifecycle) {
			((Lifecycle) handler).start();
		}
		this.running = true;
	}

	public void stop() {
		if (!this.isRunning()) {
			return;
		}
		if (this.handler instanceof Lifecycle) {
			((Lifecycle) handler).stop();
		}
		this.running = false;
	}

	public final Message<?> handle(Message<?> message) {
		if (!this.isRunning()) {
			throw new MessageHandlerNotRunningException();
		}
		for (MessageSelector selector : this.selectors) {
			if (!selector.accept(message)) {
				throw new MessageSelectorRejectedException();
			}
		}
		if (this.handler == null) {
			if (this.defaultOutputChannelName == null) {
				throw new MessagingConfigurationException(
						"endpoint must have either a 'handler' or 'defaultOutputChannelName'");
			}
			MessageChannel replyChannel = this.channelRegistry.lookupChannel(this.defaultOutputChannelName);
			replyChannel.send(message);
			return null;
		}
		try {
			Message<?> replyMessage = this.handler.handle(message);
			if (replyMessage != null) {
				this.replyHandler.handle(replyMessage, message.getHeader());
			}
		}
		catch (MessageHandlerRejectedExecutionException e) {
			throw e;
		}
		catch (Throwable t) {
			if (this.errorHandler == null) {
				throw new MessageHandlingException(
						"error occurred in endpoint, and no 'errorHandler' available", t);
			}
			this.errorHandler.handle(t);
		}
		return null;
	}

	private MessageChannel resolveReplyChannel(MessageHeader originalMessageHeader) {
		MessageChannel replyChannel = originalMessageHeader.getReplyChannel();
		if (replyChannel != null) {
			return replyChannel;
		}
		if (this.channelRegistry == null) {
			return null;
		}
		String replyChannelName = originalMessageHeader.getReplyChannelName();
		if (StringUtils.hasText(replyChannelName)) {
			return this.channelRegistry.lookupChannel(replyChannelName);
		}
		if (this.defaultOutputChannelName != null) {
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
				throw new MessageHandlingException("Unable to determine reply channel for message. " +
						"Provide a 'replyChannel' or 'replyChannelName' in the message header " +
						"or a 'defaultOutputChannelName' on the message endpoint.");
			}
			replyChannel.send(replyMessage);
		}
	}

}
