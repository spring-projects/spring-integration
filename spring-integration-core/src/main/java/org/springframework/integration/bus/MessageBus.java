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

package org.springframework.integration.bus;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.Lifecycle;
import org.springframework.integration.MessagingException;
import org.springframework.integration.adapter.AbstractTargetAdapter;
import org.springframework.integration.adapter.SourceAdapter;
import org.springframework.integration.adapter.TargetAdapter;
import org.springframework.integration.channel.ChannelRegistry;
import org.springframework.integration.channel.DefaultChannelRegistry;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.SimpleChannel;
import org.springframework.integration.dispatcher.DefaultMessageDispatcher;
import org.springframework.integration.dispatcher.DispatcherPolicy;
import org.springframework.integration.dispatcher.MessageDispatcher;
import org.springframework.integration.endpoint.ConcurrencyPolicy;
import org.springframework.integration.endpoint.MessageEndpoint;
import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.scheduling.MessagePublishingErrorHandler;
import org.springframework.integration.scheduling.MessagingTaskScheduler;
import org.springframework.integration.scheduling.MessagingTaskSchedulerAware;
import org.springframework.integration.scheduling.Schedule;
import org.springframework.integration.scheduling.SimpleMessagingTaskScheduler;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * The messaging bus. Serves as a registry for channels and endpoints, manages their lifecycle,
 * and activates subscriptions.
 * 
 * @author Mark Fisher
 */
public class MessageBus implements ChannelRegistry, ApplicationContextAware, Lifecycle {

	private Log logger = LogFactory.getLog(this.getClass());

	private ChannelRegistry channelRegistry = new DefaultChannelRegistry();

	private Map<String, MessageHandler> handlers = new ConcurrentHashMap<String, MessageHandler>();

	private Map<MessageChannel, MessageDispatcher> dispatchers = new ConcurrentHashMap<MessageChannel, MessageDispatcher>();

	private List<Lifecycle> lifecycleSourceAdapters = new CopyOnWriteArrayList<Lifecycle>();

	private MessagingTaskScheduler taskScheduler;

	private int dispatcherPoolSize = 10;

	private boolean autoCreateChannels;

	private volatile boolean initialized;

	private volatile boolean starting;

	private volatile boolean running;

	private Object lifecycleMonitor = new Object();


	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		Assert.notNull(applicationContext, "'applicationContext' must not be null");
		this.registerChannels(applicationContext);
		this.registerEndpoints(applicationContext);
		this.registerSourceAdapters(applicationContext);
		this.registerTargetAdapters(applicationContext);
		this.activateSubscriptions(applicationContext);
	}

	/**
	 * Set the size for the dispatcher thread pool.
	 */
	public void setDispatcherPoolSize(int dispatcherPoolSize) {
		Assert.isTrue(dispatcherPoolSize > 0, "'dispatcherPoolSize' must be at least 1");
		this.dispatcherPoolSize = dispatcherPoolSize;
		if (this.taskScheduler != null && this.taskScheduler instanceof SimpleMessagingTaskScheduler) {
			((SimpleMessagingTaskScheduler) this.taskScheduler).setCorePoolSize(dispatcherPoolSize);
		}
	}

	/**
	 * Set whether the bus should automatically create a channel when a
	 * subscription contains the name of a previously unregistered channel.
	 */
	public void setAutoCreateChannels(boolean autoCreateChannels) {
		this.autoCreateChannels = autoCreateChannels;
	}

	@SuppressWarnings("unchecked")
	private void registerChannels(ApplicationContext context) {
		Map<String, MessageChannel> channelBeans =
				(Map<String, MessageChannel>) context.getBeansOfType(MessageChannel.class);
		for (Map.Entry<String, MessageChannel> entry : channelBeans.entrySet()) {
			this.registerChannel(entry.getKey(), entry.getValue());
		}
	}

	@SuppressWarnings("unchecked")
	private void registerEndpoints(ApplicationContext context) {
		Map<String, MessageEndpoint> endpointBeans =
				(Map<String, MessageEndpoint>) context.getBeansOfType(MessageEndpoint.class);
		for (Map.Entry<String, MessageEndpoint> entry : endpointBeans.entrySet()) {
			this.registerEndpoint(entry.getKey(), entry.getValue());
		}
	}

	@SuppressWarnings("unchecked")
	private void registerSourceAdapters(ApplicationContext context) {
		Map<String, SourceAdapter> sourceAdapterBeans =
				(Map<String, SourceAdapter>) context.getBeansOfType(SourceAdapter.class);
		for (Map.Entry<String, SourceAdapter> entry : sourceAdapterBeans.entrySet()) {
			this.registerSourceAdapter(entry.getKey(), entry.getValue());
		}
	}

	@SuppressWarnings("unchecked")
	private void registerTargetAdapters(ApplicationContext context) {
		Map<String, TargetAdapter> targetAdapterBeans =
				(Map<String, TargetAdapter>) context.getBeansOfType(TargetAdapter.class);
		for (Map.Entry<String, TargetAdapter> entry : targetAdapterBeans.entrySet()) {
			this.registerTargetAdapter(entry.getKey(), entry.getValue());
		}
	}

	@SuppressWarnings("unchecked")
	private void activateSubscriptions(ApplicationContext context) {
		Map<String, Subscription> subscriptionBeans =
				(Map<String, Subscription>) context.getBeansOfType(Subscription.class);
		for (Subscription subscription : subscriptionBeans.values()) {
			this.activateSubscription(subscription);
			if (logger.isInfoEnabled()) {
				logger.info("activated subscription to channel '" + subscription.getChannel() + 
						"' for handler '" + subscription.getHandler() + "'");
			}
		}
	}

	public void initialize() {
		if (this.getInvalidMessageChannel() == null) {
			this.setInvalidMessageChannel(new SimpleChannel(Integer.MAX_VALUE));
		}
		initScheduler();
		this.initialized = true;
	}

	private void initScheduler() {
		CustomizableThreadFactory threadFactory = new CustomizableThreadFactory();
		threadFactory.setThreadNamePrefix("dispatcher-executor-");
		threadFactory.setThreadGroup(new ThreadGroup("dispatcher-executors"));
		SimpleMessagingTaskScheduler scheduler = new SimpleMessagingTaskScheduler();
		scheduler.setCorePoolSize(this.dispatcherPoolSize);
		scheduler.setThreadFactory(threadFactory);
		scheduler.setErrorHandler(new MessagePublishingErrorHandler(this.getInvalidMessageChannel()));
		scheduler.afterPropertiesSet();
		this.taskScheduler = scheduler;
	}

	public MessageChannel getInvalidMessageChannel() {
		return this.channelRegistry.getInvalidMessageChannel();
	}

	public void setInvalidMessageChannel(MessageChannel invalidMessageChannel) {
		this.channelRegistry.setInvalidMessageChannel(invalidMessageChannel);
	}

	public MessageChannel lookupChannel(String channelName) {
		return this.channelRegistry.lookupChannel(channelName);
	}

	public void registerChannel(String name, MessageChannel channel) {
		this.registerChannel(name, channel, null);
	}

	public void registerChannel(String name, MessageChannel channel, DispatcherPolicy dispatcherPolicy) {
		if (!this.initialized) {
			this.initialize();
		}
		DefaultMessageDispatcher dispatcher = new DefaultMessageDispatcher(channel);
		dispatcher.setMessagingTaskScheduler(this.taskScheduler);
		if (dispatcherPolicy != null) {
			dispatcher.setMaxMessagesPerTask(dispatcherPolicy.getMaxMessagesPerTask());
			dispatcher.setReceiveTimeout(dispatcherPolicy.getReceiveTimeout());
			dispatcher.setRejectionLimit(dispatcherPolicy.getRejectionLimit());
			dispatcher.setRetryInterval(dispatcherPolicy.getRetryInterval());
		}
		this.dispatchers.put(channel, dispatcher);
		this.channelRegistry.registerChannel(name, channel);
	}

	public void registerEndpoint(String name, MessageEndpoint endpoint) {
		if (!this.initialized) {
			this.initialize();
		}
		Assert.notNull(name, "'name' must not be null");
		Assert.notNull(endpoint, "'endpoint' must not be null");
		endpoint.setName(name);
		this.handlers.put(name, endpoint);
		endpoint.setChannelRegistry(this);
		Schedule schedule = endpoint.getSchedule();
		if (endpoint.getInputChannelName() != null) {
			Subscription subscription = new Subscription();
			subscription.setHandler(name);
			subscription.setChannel(endpoint.getInputChannelName());
			if (schedule != null) {
				subscription.setSchedule(schedule);
			}
			this.activateSubscription(subscription);
		}
		if (this.autoCreateChannels) {
			String defaultOutputChannelName = endpoint.getDefaultOutputChannelName();
			if (StringUtils.hasText(defaultOutputChannelName) && this.lookupChannel(defaultOutputChannelName) == null) {
				this.registerChannel(defaultOutputChannelName, new SimpleChannel());
			}
		}
	}

	public void registerSourceAdapter(String name, SourceAdapter adapter) {
		if (!this.initialized) {
			this.initialize();
		}
		if (adapter instanceof MessagingTaskSchedulerAware) {
			((MessagingTaskSchedulerAware) adapter).setMessagingTaskScheduler(this.taskScheduler);
		}
		if (adapter instanceof Lifecycle) {
			this.lifecycleSourceAdapters.add((Lifecycle) adapter);
			if (this.isRunning()) {
				((Lifecycle) adapter).start();
			}
		}
		if (logger.isInfoEnabled()) {
			logger.info("registered source adapter '" + name + "'");
		}
	}

	public void registerTargetAdapter(String name, TargetAdapter targetAdapter) {
		if (targetAdapter instanceof AbstractTargetAdapter) {
			AbstractTargetAdapter<?> adapter = (AbstractTargetAdapter<?>) targetAdapter;
			adapter.setName(name);
			this.handlers.put(name, adapter);
			MessageChannel channel = adapter.getChannel();
			Schedule schedule = adapter.getSchedule();
			ConcurrencyPolicy concurrencyPolicy = new ConcurrencyPolicy();
			concurrencyPolicy.setCoreConcurrency(1);
			concurrencyPolicy.setMaxConcurrency(1);
			this.doActivate(channel, adapter, schedule, concurrencyPolicy);
		}
		if (logger.isInfoEnabled()) {
			logger.info("registered target adapter '" + name + "'");
		}
	}

	public void activateSubscription(Subscription subscription) {
		String channelName = subscription.getChannel();
		String handlerName = subscription.getHandler();
		Schedule schedule = subscription.getSchedule();
		ConcurrencyPolicy concurrencyPolicy = subscription.getConcurrencyPolicy();
		MessageHandler handler = this.handlers.get(handlerName);
		if (handler == null) {
			throw new MessagingException("Cannot activate subscription, unknown handler '" + handlerName + "'");
		}
		MessageChannel channel = this.lookupChannel(channelName);
		if (channel == null) {
			if (this.autoCreateChannels == false) {
				throw new MessagingException("Cannot activate subscription, unknown channel '" + channelName +
						"'. Consider enabling the 'autoCreateChannels' option for the message bus.");
			}
			if (this.logger.isInfoEnabled()) {
				logger.info("auto-creating channel '" + channelName + "'");
			}
			channel = new SimpleChannel(); 
			this.registerChannel(channelName, channel);
		}
		this.doActivate(channel, handler, schedule, concurrencyPolicy);
		if (logger.isInfoEnabled()) {
			logger.info("activated subscription to channel '" + channelName + 
					"' for handler '" + handlerName + "'");
		}
	}

	private void doActivate(MessageChannel channel, MessageHandler handler, Schedule schedule, ConcurrencyPolicy concurrencyPolicy) {
		MessageDispatcher dispatcher = dispatchers.get(channel);
		if (dispatcher == null) {
			if (logger.isWarnEnabled()) {
				logger.warn("no dispatcher available for channel '" + channel + "', be sure to register the channel");
			}
		}
		if (concurrencyPolicy != null) {
			handler = new PooledMessageHandler(handler, concurrencyPolicy.getCoreConcurrency(), concurrencyPolicy.getMaxConcurrency());
		}
		dispatcher.addHandler(handler, schedule);
		if (this.isRunning() && !dispatcher.isRunning()) {
			dispatcher.start();
		}
	}

	public boolean isRunning() {
		synchronized (this.lifecycleMonitor) {
			return this.running;
		}
	}

	public void start() {
		if (!this.initialized) {
			this.initialize();
		}
		if (this.isRunning() || this.starting) {
			return;
		}
		this.starting = true;
		synchronized (this.lifecycleMonitor) {
			this.taskScheduler.start();
			this.running = true;
			for (MessageDispatcher dispatcher : this.dispatchers.values()) {
				dispatcher.start();
				if (logger.isInfoEnabled()) {
					logger.info("started dispatcher '" + dispatcher + "'");
				}
			}
			for (Lifecycle adapter : this.lifecycleSourceAdapters) {
				adapter.start();
				if (logger.isInfoEnabled()) {
					logger.info("started source adapter '" + adapter + "'");
				}
			}
		}
		this.running = true;
		this.starting = false;
		if (logger.isInfoEnabled()) {
			logger.info("message bus started");
		}
	}

	public void stop() {
		synchronized (this.lifecycleMonitor) {
			if (this.isRunning()) {
				this.running = false;
				this.taskScheduler.stop();
				for (Lifecycle adapter : this.lifecycleSourceAdapters) {
					adapter.stop();
					if (logger.isInfoEnabled()) {
						logger.info("stopped source adapter '" + adapter + "'");
					}
				}
				for (MessageDispatcher dispatcher : this.dispatchers.values()) {
					dispatcher.stop();
					if (logger.isInfoEnabled()) {
						logger.info("stopped dispatcher '" + dispatcher + "'");
					}
				}
			}
		}
	}

}
