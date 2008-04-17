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
import org.springframework.beans.factory.DisposableBean;
import org.springframework.integration.channel.ChannelRegistry;
import org.springframework.integration.channel.ChannelRegistryAware;
import org.springframework.integration.handler.MessageHandlerNotRunningException;
import org.springframework.integration.handler.MessageHandlerRejectedExecutionException;
import org.springframework.integration.message.Message;
import org.springframework.integration.message.MessageHandlingException;
import org.springframework.integration.message.Target;
import org.springframework.integration.message.selector.MessageSelector;
import org.springframework.integration.message.selector.MessageSelectorRejectedException;
import org.springframework.integration.scheduling.Subscription;
import org.springframework.integration.util.ErrorHandler;
import org.springframework.util.Assert;

/**
 * Base class for {@link MessageEndpoint} implementations.
 * 
 * @author Mark Fisher
 */
public class TargetEndpoint implements MessageEndpoint, BeanNameAware {

	protected final Log logger = LogFactory.getLog(this.getClass());

	private volatile String name;

	private volatile Target target;

	private volatile Subscription subscription;

	private volatile ConcurrencyPolicy concurrencyPolicy;

	private volatile ErrorHandler errorHandler;

	private final List<MessageSelector> selectors = new CopyOnWriteArrayList<MessageSelector>();

	private volatile ChannelRegistry channelRegistry;

	private volatile boolean initialized;

	private volatile boolean running;


	public TargetEndpoint() {
	}

	public TargetEndpoint(Target target) {
		Assert.notNull(target, "target must not be null");
		this.target = target;
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

	public Target getTarget() {
		return this.target;
	}

	public void setTarget(Target target) {
		Assert.notNull(target, "target must not be null");
		this.target = target;
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

	/**
	 * Set the channel registry to use for looking up channels by name.
	 */
	public void setChannelRegistry(ChannelRegistry channelRegistry) {
		this.channelRegistry = channelRegistry;
	}

	protected ChannelRegistry getChannelRegistry() {
		return this.channelRegistry;
	}

	public void afterPropertiesSet() {
		if (this.target instanceof ChannelRegistryAware) {
			((ChannelRegistryAware) this.target).setChannelRegistry(this.channelRegistry);
		}
		if (this.concurrencyPolicy != null && !(this.target instanceof ConcurrentTarget)) {
			int capacity = this.concurrencyPolicy.getQueueCapacity();
			BlockingQueue<Runnable> queue = (capacity < 1) ? new SynchronousQueue<Runnable>() : new ArrayBlockingQueue<Runnable>(capacity);
			ExecutorService executor = new ThreadPoolExecutor(this.concurrencyPolicy.getCoreSize(), this.concurrencyPolicy.getMaxSize(),
					this.concurrencyPolicy.getKeepAliveSeconds(), TimeUnit.SECONDS, queue);
			this.target = new ConcurrentTarget(this.target, executor);
		}
		if (this.target instanceof ConcurrentTarget) {
			if (this.errorHandler != null) {
				((ConcurrentTarget) this.target).setErrorHandler(this.errorHandler);
			}
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
		if (!this.initialized) {
			this.afterPropertiesSet();
		}
		this.running = true;
	}

	public void stop() {
		if (!this.isRunning()) {
			return;
		}
		if (this.target instanceof DisposableBean) {
			try {
				((DisposableBean) this.target).destroy();
			}
			catch (Exception e) {
				if (logger.isWarnEnabled()) {
					logger.warn("exception occurred when destroying target", e);
				}
			}
		}
		this.running = false;
	}

	public final boolean send(Message<?> message) {
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
			return this.target.send(message);
		}
		catch (MessageHandlerRejectedExecutionException e) {
			throw e;
		}
		catch (Throwable t) {
			if (this.errorHandler == null) {
				if (t instanceof MessageHandlingException) {
					throw (MessageHandlingException) t;
				}
				throw new MessageHandlingException(message,
						"error occurred in endpoint, and no 'errorHandler' available", t);
			}
			this.errorHandler.handle(t);
			return false;
		}
	}

	public String toString() {
		return (this.name != null) ? this.name : super.toString();
	}

}
