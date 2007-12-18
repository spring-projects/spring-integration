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
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.Lifecycle;
import org.springframework.integration.MessagingException;
import org.springframework.integration.channel.ChannelRegistry;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.PointToPointChannel;
import org.springframework.integration.channel.DefaultChannelRegistry;
import org.springframework.integration.endpoint.MessageEndpoint;
import org.springframework.integration.message.Message;
import org.springframework.util.Assert;

/**
 * The messaging bus. Serves as a registry for channels and endpoints, manages their lifecycle,
 * and activates subscriptions.
 * 
 * @author Mark Fisher
 */
public class MessageBus implements ChannelRegistry, ApplicationContextAware, Lifecycle {

	private Log logger = LogFactory.getLog(this.getClass());

	private ChannelRegistry channelRegistry = new DefaultChannelRegistry();

	private Map<String, MessageEndpoint> endpoints = new ConcurrentHashMap<String, MessageEndpoint>();

	private List<DispatcherTask> dispatcherTasks = new CopyOnWriteArrayList<DispatcherTask>();

	private Map<MessageEndpoint, EndpointExecutor> endpointExecutors = new ConcurrentHashMap<MessageEndpoint, EndpointExecutor>();

	private ScheduledThreadPoolExecutor dispatcherExecutor;

	private boolean autoCreateChannels;

	private boolean running;

	private Object lifecycleMonitor = new Object();


	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		Assert.notNull(applicationContext, "applicationContext must not be null");
		this.registerChannels(applicationContext);
		this.registerEndpoints(applicationContext);
		this.activateSubscriptions(applicationContext);
	}

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
		this.dispatcherExecutor = new ScheduledThreadPoolExecutor(this.dispatcherTasks.size() > 0 ? this.dispatcherTasks.size() : 1);
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

	public void registerEndpoint(String name, MessageEndpoint endpoint) {
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
			channel = new PointToPointChannel(); 
			this.registerChannel(channelName, channel);
		}
		MessageEndpoint endpoint = this.endpoints.get(endpointName);
		if (endpoint == null) {
			throw new MessagingException("Cannot activate subscription, unknown endpoint '" + endpointName + "'");
		}
		if (logger.isInfoEnabled()) {
			logger.info("activated subscription to channel '" + channelName + 
					"' for endpoint '" + endpointName + "'");
		}
		EndpointExecutor endpointExecutor = new EndpointExecutor(policy.getConcurrency(), policy.getMaxConcurrency());
		endpointExecutors.put(endpoint, endpointExecutor);
		DispatcherTask dispatcherTask = new DispatcherTask(channel, endpoint, policy);
		this.dispatcherTasks.add(dispatcherTask);
		if (this.logger.isInfoEnabled()) {
			logger.info("registered dispatcher task: channel='" +
					channelName + "' endpoint='" + endpointName + "'");
		}
		if (this.isRunning()) {
			scheduleDispatcherTask(dispatcherTask);
			if (this.logger.isInfoEnabled()) {
				logger.info("scheduled dispatcher task: channel='" +
						channelName + "' endpoint='" + endpointName + "'");
			}
		}
	}

	public int getActiveCountForEndpoint(String endpointName) {
		MessageEndpoint endpoint = this.endpoints.get(endpointName);
		if (endpoint != null) {
			EndpointExecutor executor = this.endpointExecutors.get(endpoint);
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
			}
		}
	}


	private class DispatcherTask implements Runnable {

		private MessageChannel channel;

		private MessageEndpoint endpoint;

		private ConsumerPolicy policy;


		public DispatcherTask(MessageChannel channel, MessageEndpoint endpoint, ConsumerPolicy policy) {
			this.channel = channel;
			this.endpoint = endpoint;
			this.policy = policy;
		}

		public MessageChannel getChannel() {
			return this.channel;
		}

		public MessageEndpoint getEndpoint() {
			return this.endpoint;
		}

		public ConsumerPolicy getPolicy() {
			return this.policy;
		}

		public void run() {
			EndpointExecutor executor = endpointExecutors.get(this.endpoint);
			if (executor == null || executor.isShutdown()) {
				if (logger.isWarnEnabled()) {
					logger.warn("dispatcher shutting down, endpoint executor is not active");
				}
				return;
			}			
			for (int i = 0; i < policy.getMaxMessagesPerTask(); i++) {
				Message<?> message = channel.receive(this.policy.getReceiveTimeout());
				if (message == null) {
					return;
				}
				else {
					boolean taskSubmitted = false;
					int attempts = 0;
					while (!taskSubmitted) {
						try {
							executor.execute(new EndpointTask(this.endpoint, message));
							taskSubmitted = true;
						}
						catch (RejectedExecutionException rex) {
							attempts++;
							if (attempts == policy.getRejectionLimit()) {
								attempts = 0;
								if (logger.isDebugEnabled()) {
									logger.debug("reached rejected execution limit");
								}
								try {
									Thread.sleep(policy.getRejectionLimitWait());
								}
								catch (InterruptedException iex) {
									Thread.currentThread().interrupt();
								}
							}
						}
					}
				}
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


	public static class EndpointTask implements Runnable {

		private MessageEndpoint endpoint;

		private Message<?> message;

		private Throwable error;


		EndpointTask(MessageEndpoint endpoint, Message<?> message) {
			this.endpoint = endpoint;
			this.message = message;
		}

		public Throwable getError() {
			return this.error;
		}

		public void run() {
			try {
				this.endpoint.messageReceived(this.message);
			}
			catch (Throwable t) {
				this.error = t;
			}
		}
	}

}
