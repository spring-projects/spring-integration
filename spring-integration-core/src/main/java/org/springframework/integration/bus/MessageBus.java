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
import java.util.concurrent.ScheduledThreadPoolExecutor;

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
import org.springframework.integration.endpoint.MessageEndpoint;
import org.springframework.integration.message.ErrorMessage;
import org.springframework.integration.message.MessageReceiver;
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

	private Map<String, MessageEndpoint<?>> endpoints = new ConcurrentHashMap<String, MessageEndpoint<?>>();

	private Map<String, TargetAdapter<?>> targetAdapters = new ConcurrentHashMap<String, TargetAdapter<?>>();

	private Map<String, Lifecycle> lifecycleComponents = new ConcurrentHashMap<String, Lifecycle>();

	private List<DispatcherTask> dispatcherTasks = new CopyOnWriteArrayList<DispatcherTask>();

	private Map<MessageReceiver<?>, MessageReceivingExecutor> receiverExecutors = new ConcurrentHashMap<MessageReceiver<?>, MessageReceivingExecutor>();

	private ScheduledThreadPoolExecutor dispatcherExecutor;

	private int dispatcherPoolSize = 10;

	private boolean autoCreateChannels;

	private boolean running;

	private Object lifecycleMonitor = new Object();


	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		Assert.notNull(applicationContext, "applicationContext must not be null");
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
		if (this.dispatcherExecutor != null) {
			this.dispatcherExecutor.setCorePoolSize(dispatcherPoolSize);
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
						"' for endpoint '" + subscription.getEndpoint() + "'");
			}
		}
	}

	public void initialize() {
		initDispatcherExecutor();
		if (this.getInvalidMessageChannel() == null) {
			this.setInvalidMessageChannel(new SimpleChannel(Integer.MAX_VALUE));
		}
	}

	private void initDispatcherExecutor() {
		CustomizableThreadFactory threadFactory = new CustomizableThreadFactory();
		threadFactory.setThreadNamePrefix("dispatcher-executor-");
		threadFactory.setThreadGroup(new ThreadGroup("dispatcher-executors"));
		this.dispatcherExecutor = new ScheduledThreadPoolExecutor(this.dispatcherPoolSize, threadFactory);
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
		this.channelRegistry.registerChannel(name, channel);
	}

	public void registerEndpoint(String name, MessageEndpoint<?> endpoint) {
		Assert.notNull(name, "'name' must not be null");
		Assert.notNull(endpoint, "'endpoint' must not be null");
		this.endpoints.put(name, endpoint);
		if (logger.isInfoEnabled()) {
			logger.info("registered endpoint '" + name + "'");
		}
		endpoint.setChannelRegistry(this);
		if (endpoint.getInputChannelName() != null && endpoint.getConsumerPolicy() != null) {
			Subscription subscription = new Subscription();
			subscription.setChannel(endpoint.getInputChannelName());
			subscription.setEndpoint(name);
			subscription.setPolicy(endpoint.getConsumerPolicy());
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
		if (adapter instanceof MessageDispatcher) {
			MessageDispatcher dispatcher = (MessageDispatcher) adapter;
			ConsumerPolicy policy = dispatcher.getConsumerPolicy();
			DispatcherTask dispatcherTask = new DispatcherTask(dispatcher, policy);
			this.addDispatcherTask(dispatcherTask);
		}
		if (adapter instanceof Lifecycle) {
			this.addLifecycleComponent(name, (Lifecycle) adapter);
		}
		if (logger.isInfoEnabled()) {
			logger.info("registered source adapter '" + name + "'");
		}
	}

	public void registerTargetAdapter(String name, TargetAdapter<?> targetAdapter) {
		if (targetAdapter instanceof AbstractTargetAdapter) {
			AbstractTargetAdapter<?> adapter = (AbstractTargetAdapter<?>) targetAdapter;
			adapter.setName(name);
			this.targetAdapters.put(name, targetAdapter);
			MessageChannel channel = adapter.getChannel();
			ConsumerPolicy policy = adapter.getConsumerPolicy();
			MessageRetriever retriever = new ChannelPollingMessageRetriever(channel, policy);
			UnicastMessageDispatcher dispatcher = new UnicastMessageDispatcher(retriever, policy);
			MessageReceivingExecutor executor = new MessageReceivingExecutor(adapter, policy.getConcurrency(), policy.getMaxConcurrency());
			dispatcher.addExecutor(executor);
			this.addLifecycleComponent(name + "-executor", executor);
			this.addDispatcherTask(new DispatcherTask(dispatcher, policy));
			if (logger.isInfoEnabled()) {
				logger.info("registered target adapter '" + name + "'");
			}
		}
	}

	public void activateSubscription(Subscription subscription) {
		String channelName = subscription.getChannel();
		String endpointName = subscription.getEndpoint();
		ConsumerPolicy policy = subscription.getPolicy();
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
		MessageEndpoint<?> endpoint = this.endpoints.get(endpointName);
		if (endpoint == null) {
			throw new MessagingException("Cannot activate subscription, unknown endpoint '" + endpointName + "'");
		}
		if (logger.isInfoEnabled()) {
			logger.info("activated subscription to channel '" + channelName + 
					"' for endpoint '" + endpointName + "'");
		}
		MessageReceivingExecutor executor = new MessageReceivingExecutor(endpoint, policy.getConcurrency(), policy.getMaxConcurrency());
		this.receiverExecutors.put(endpoint, executor);
		this.lifecycleComponents.put(endpointName + "-executor", executor);
		MessageRetriever retriever = new ChannelPollingMessageRetriever(channel, policy);
		UnicastMessageDispatcher dispatcher = new UnicastMessageDispatcher(retriever, policy);
		dispatcher.addExecutor(executor);
		DispatcherTask dispatcherTask = new DispatcherTask(dispatcher, policy);
		if (this.isRunning()) {
			executor.start();
		}
		this.addDispatcherTask(dispatcherTask);
		if (this.logger.isInfoEnabled()) {
			logger.info("registered dispatcher task: channel='" +
					channelName + "' receiver='" + endpointName + "'");
		}
	}

	private void addLifecycleComponent(String name, Lifecycle component) {
		this.lifecycleComponents.put(name, component);
		if (this.isRunning()) {
			component.start();
			if (logger.isInfoEnabled()) {
				logger.info("started lifecycle component '" + name + "'");
			}
		}
	}

	private void addDispatcherTask(DispatcherTask dispatcherTask) {
		this.dispatcherTasks.add(dispatcherTask);
		if (this.isRunning()) {
			scheduleDispatcherTask(dispatcherTask);
			if (this.logger.isInfoEnabled()) {
				logger.info("scheduled dispatcher task");
			}
		}
	}

	public int getActiveCountForReceiver(String receiverName) {
		MessageReceiver<?> receiver = this.endpoints.get(receiverName);
		if (receiver == null) {
			receiver = this.targetAdapters.get(receiverName);
		}
		if (receiver != null) {
			MessageReceivingExecutor executor = this.receiverExecutors.get(receiver);
			if (executor != null) {
				return executor.getActiveCount();
			}
		}
		return 0;
	}

	private void scheduleDispatcherTask(DispatcherTask task) {
		ConsumerPolicy policy = task.getPolicy();
		if (policy.getPeriod() <= 0) {
			if (policy.getReceiveTimeout() <= 0) {
				if (logger.isWarnEnabled()) {
					logger.warn("Scheduling a repeating task with no receive timeout is not recommended! " +
							"Consider providing a positive value for either 'period' or 'receiveTimeout'");
				}
			}
			dispatcherExecutor.schedule(new RepeatingDispatcherTask(task), policy.getInitialDelay(), policy.getTimeUnit());
		}
		else if (policy.isFixedRate()) {
			dispatcherExecutor.scheduleAtFixedRate(task, policy.getInitialDelay(), policy.getPeriod(), policy.getTimeUnit());
		}
		else {
			dispatcherExecutor.scheduleWithFixedDelay(task, policy.getInitialDelay(), policy.getPeriod(), policy.getTimeUnit());
		}
	}

	public boolean isRunning() {
		synchronized (this.lifecycleMonitor) {
			return this.running;
		}
	}

	public void start() {
		if (this.dispatcherExecutor == null) {
			this.initialize();
		}
		synchronized (this.lifecycleMonitor) {
			if (!this.isRunning()) {
				this.running = true;
				for (Map.Entry<String, Lifecycle> entry : this.lifecycleComponents.entrySet()) {
					entry.getValue().start();
					if (logger.isInfoEnabled()) {
						logger.info("started lifecycle component '" + entry.getKey() + "'");
					}
				}
				for (DispatcherTask task : this.dispatcherTasks) {
					scheduleDispatcherTask(task);
				}
			}
		}
	}

	public void stop() {
		synchronized (this.lifecycleMonitor) {
			if (this.isRunning()) {
				this.running = false;
				this.dispatcherExecutor.shutdownNow();
				for (Map.Entry<String, Lifecycle> entry : this.lifecycleComponents.entrySet()) {
					entry.getValue().stop();
					if (logger.isInfoEnabled()) {
						logger.info("stopped lifecycle component '" + entry.getKey() + "'");
					}
				}
			}
		}
	}

	private void handleDispatchError(DispatcherTask task, Throwable t) {
		try {
			this.getInvalidMessageChannel().send(new ErrorMessage(t), 1000);
		}
		catch (Throwable ignore) { // message will be logged only
		}
		if (logger.isWarnEnabled()) {
			logger.warn("failure occurred while dispatching message", t);
		}
	}


	private class DispatcherTask implements Runnable {

		private MessageDispatcher dispatcher;

		private ConsumerPolicy policy;


		public DispatcherTask(MessageDispatcher dispatcher, ConsumerPolicy policy) {
			this.dispatcher = dispatcher;
			this.policy = policy;
		}


		public ConsumerPolicy getPolicy() {
			return this.policy;
		}

		public void run() {
			try {
				dispatcher.dispatch();
			}
			catch (Throwable t) {
				handleDispatchError(this, t);
			}
		}

	}


	private class RepeatingDispatcherTask implements Runnable {

		private DispatcherTask task;

		RepeatingDispatcherTask(DispatcherTask task) {
			this.task = task;
		}

		public void run() {
			task.run();
			dispatcherExecutor.execute(new RepeatingDispatcherTask(task));
		}
	}

}
