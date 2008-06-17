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

package org.springframework.integration.bus;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.Lifecycle;
import org.springframework.context.event.ApplicationEventMulticaster;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.SimpleApplicationEventMulticaster;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.integration.ConfigurationException;
import org.springframework.integration.bus.interceptor.MessageBusInterceptor;
import org.springframework.integration.channel.ChannelRegistry;
import org.springframework.integration.channel.ChannelRegistryAware;
import org.springframework.integration.channel.DefaultChannelRegistry;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.QueueChannel;
import org.springframework.integration.channel.factory.ChannelFactory;
import org.springframework.integration.channel.factory.QueueChannelFactory;
import org.springframework.integration.endpoint.ConcurrencyPolicy;
import org.springframework.integration.endpoint.DefaultEndpointRegistry;
import org.springframework.integration.endpoint.EndpointRegistry;
import org.springframework.integration.endpoint.HandlerEndpoint;
import org.springframework.integration.endpoint.MessageEndpoint;
import org.springframework.integration.endpoint.MessagingGateway;
import org.springframework.integration.endpoint.SourceEndpoint;
import org.springframework.integration.endpoint.TargetEndpoint;
import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.message.Target;
import org.springframework.integration.scheduling.MessagePublishingErrorHandler;
import org.springframework.integration.scheduling.MessagingTaskScheduler;
import org.springframework.integration.scheduling.Schedule;
import org.springframework.integration.scheduling.SimpleMessagingTaskScheduler;
import org.springframework.integration.scheduling.Subscription;
import org.springframework.util.Assert;

/**
 * The messaging bus. Serves as a registry for channels and endpoints, manages their lifecycle,
 * and activates subscriptions.
 * 
 * @author Mark Fisher
 * @author Marius Bogoevici
 */
public class MessageBus implements ChannelRegistry, EndpointRegistry, ApplicationContextAware, ApplicationListener,
		Lifecycle {

	public static final String ERROR_CHANNEL_NAME = "errorChannel";

	private static final int DEFAULT_DISPATCHER_POOL_SIZE = 10;

	private final Log logger = LogFactory.getLog(this.getClass());

	private volatile ChannelFactory channelFactory = new QueueChannelFactory();

	private final ChannelRegistry channelRegistry = new DefaultChannelRegistry();

	private final EndpointRegistry endpointRegistry = new DefaultEndpointRegistry();

	private final Map<MessageChannel, SubscriptionManager> subscriptionManagers = new ConcurrentHashMap<MessageChannel, SubscriptionManager>();

	private final List<Lifecycle> lifecycleEndpoints = new CopyOnWriteArrayList<Lifecycle>();

	private final MessageBusInterceptorsList interceptors = new MessageBusInterceptorsList();

	private volatile MessagingTaskScheduler taskScheduler;

	private volatile ScheduledExecutorService executor;

	private volatile ConcurrencyPolicy defaultConcurrencyPolicy;

	private volatile boolean configureAsyncEventMulticaster = false;

	private volatile boolean autoCreateChannels = false;

	private volatile boolean autoStartup = true;

	private volatile boolean initialized;

	private volatile boolean initializing;

	private volatile boolean starting;

	private volatile boolean running;

	private final Object lifecycleMonitor = new Object();

	/**
	 * Set the {@link ChannelFactory} to use for auto-creating channels.
	 */
	public void setChannelFactory(ChannelFactory channelFactory) {
		this.channelFactory = channelFactory;
	}

	public ChannelFactory getChannelFactory() {
		return channelFactory;
	}

	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		Assert.notNull(applicationContext, "'applicationContext' must not be null");
		if (applicationContext.getBeanNamesForType(this.getClass()).length > 1) {
			throw new ConfigurationException("Only one instance of '" + this.getClass().getSimpleName()
					+ "' is allowed per ApplicationContext.");
		}
		this.registerChannels(applicationContext);
	}

	/**
	 * Set the {@link ScheduledExecutorService} to use for scheduling message dispatchers.
	 */
	public void setScheduledExecutorService(ScheduledExecutorService executor) {
		this.executor = executor;
	}

	/**
	 * Specify the default concurrency policy to be used for any endpoint that
	 * is registered without an explicitly provided policy of its own.
	 */
	public void setDefaultConcurrencyPolicy(ConcurrencyPolicy defaultConcurrencyPolicy) {
		this.defaultConcurrencyPolicy = defaultConcurrencyPolicy;
	}

	/**
	 * Set whether to automatically start the bus after initialization.
	 * <p>Default is 'true'; set this to 'false' to allow for manual startup
	 * through the {@link #start()} method.
	 */
	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	/**
	 * Set whether the bus should automatically create a channel when a
	 * subscription contains the name of a previously unregistered channel.
	 */
	public void setAutoCreateChannels(boolean autoCreateChannels) {
		this.autoCreateChannels = autoCreateChannels;
	}

	/**
	 * Set whether the bus should configure its asynchronous task executor
	 * to also be used by the ApplicationContext's 'applicationEventMulticaster'.
	 * This will only apply if the multicaster defined within the context
	 * is an instance of SimpleApplicationEventMulticaster (the default).
	 * This property is 'false' by default. 
	 */
	public void setConfigureAsyncEventMulticaster(boolean configureAsyncEventMulticaster) {
		this.configureAsyncEventMulticaster = configureAsyncEventMulticaster;
	}

	@SuppressWarnings("unchecked")
	private void registerChannels(ApplicationContext context) {
		Map<String, MessageChannel> channelBeans = (Map<String, MessageChannel>) context
				.getBeansOfType(MessageChannel.class);
		for (Map.Entry<String, MessageChannel> entry : channelBeans.entrySet()) {
			this.registerChannel(entry.getKey(), entry.getValue());
		}
	}

	@SuppressWarnings("unchecked")
	private void registerEndpoints(ApplicationContext context) {
		Map<String, MessageEndpoint> endpointBeans = (Map<String, MessageEndpoint>) context
				.getBeansOfType(MessageEndpoint.class);
		for (Map.Entry<String, MessageEndpoint> entry : endpointBeans.entrySet()) {
			this.registerEndpoint(entry.getKey(), entry.getValue());
		}
	}

	@SuppressWarnings("unchecked")
	private void registerGateways(ApplicationContext context) {
		Map<String, MessagingGateway> gatewayBeans = (Map<String, MessagingGateway>) context
				.getBeansOfType(MessagingGateway.class);
		for (Map.Entry<String, MessagingGateway> entry : gatewayBeans.entrySet()) {
			this.registerGateway(entry.getKey(), entry.getValue());
		}
	}

	public void initialize() {
		synchronized (this.lifecycleMonitor) {
			if (this.initialized || this.initializing) {
				return;
			}
			this.initializing = true;
			if (this.executor == null) {
				this.executor = new ScheduledThreadPoolExecutor(DEFAULT_DISPATCHER_POOL_SIZE);
			}
			this.taskScheduler = new SimpleMessagingTaskScheduler(this.executor);
			if (this.getErrorChannel() == null) {
				this.setErrorChannel(new DefaultErrorChannel());
			}
			this.taskScheduler.setErrorHandler(new MessagePublishingErrorHandler(this.getErrorChannel()));
			this.initialized = true;
			this.initializing = false;
		}
	}

	public MessageChannel getErrorChannel() {
		return this.lookupChannel(ERROR_CHANNEL_NAME);
	}

	public void setErrorChannel(MessageChannel errorChannel) {
		this.registerChannel(ERROR_CHANNEL_NAME, errorChannel);
	}

	public MessageChannel lookupChannel(String channelName) {
		return this.channelRegistry.lookupChannel(channelName);
	}

	public void registerChannel(String name, MessageChannel channel) {
		if (!this.initialized) {
			this.initialize();
		}
		channel.setName(name);
		SubscriptionManager manager = new SubscriptionManager(channel, this.taskScheduler);
		this.subscriptionManagers.put(channel, manager);
		this.channelRegistry.registerChannel(name, channel);
		if (logger.isInfoEnabled()) {
			logger.info("registered channel '" + name + "'");
		}
	}

	public MessageChannel unregisterChannel(String name) {
		MessageChannel removedChannel = this.channelRegistry.unregisterChannel(name);
		if (removedChannel != null) {
			SubscriptionManager manager = this.subscriptionManagers.remove(removedChannel);
			if (manager != null && manager.isRunning()) {
				manager.stop();
			}
		}
		return removedChannel;
	}

	public void registerHandler(String name, MessageHandler handler, Subscription subscription) {
		this.registerHandler(name, handler, subscription, this.defaultConcurrencyPolicy);
	}

	public void registerHandler(String name, MessageHandler handler, Subscription subscription,
			ConcurrencyPolicy concurrencyPolicy) {
		Assert.notNull(handler, "'handler' must not be null");
		HandlerEndpoint endpoint = new HandlerEndpoint(handler);
		this.doRegisterEndpoint(name, endpoint, subscription, concurrencyPolicy);
	}

	public void registerTarget(String name, Target target, Subscription subscription) {
		this.registerTarget(name, target, subscription, this.defaultConcurrencyPolicy);
	}

	public void registerTarget(String name, Target target, Subscription subscription,
			ConcurrencyPolicy concurrencyPolicy) {
		Assert.notNull(target, "'target' must not be null");
		TargetEndpoint endpoint = new TargetEndpoint(target);
		this.doRegisterEndpoint(name, endpoint, subscription, concurrencyPolicy);
	}

	private void doRegisterEndpoint(String name, TargetEndpoint endpoint, Subscription subscription,
			ConcurrencyPolicy concurrencyPolicy) {
		endpoint.setName(name);
		endpoint.setSubscription(subscription);
		endpoint.setConcurrencyPolicy(concurrencyPolicy);
		this.registerEndpoint(name, endpoint);
	}

	public void registerEndpoint(String name, MessageEndpoint endpoint) {
		if (!this.initialized) {
			this.initialize();
		}
		if (endpoint instanceof ChannelRegistryAware) {
			((ChannelRegistryAware) endpoint).setChannelRegistry(this.channelRegistry);
		}
		if (endpoint instanceof TargetEndpoint) {
			this.registerTargetEndpoint((TargetEndpoint) endpoint);
		}
		else if (endpoint instanceof SourceEndpoint) {
			this.registerSourceEndpoint(name, (SourceEndpoint) endpoint);
		}
		this.endpointRegistry.registerEndpoint(name, endpoint);
		if (this.isRunning()) {
			activateEndpoint(endpoint);
		}
		if (logger.isInfoEnabled()) {
			logger.info("registered endpoint '" + name + "'");
		}
	}

	private void registerTargetEndpoint(TargetEndpoint endpoint) {
		if (endpoint.getConcurrencyPolicy() == null && this.defaultConcurrencyPolicy != null) {
			endpoint.setConcurrencyPolicy(this.defaultConcurrencyPolicy);
		}
	}

	public MessageEndpoint unregisterEndpoint(String name) {
		MessageEndpoint endpoint = this.endpointRegistry.unregisterEndpoint(name);
		if (endpoint == null) {
			return null;
		}
		if (endpoint instanceof TargetEndpoint) {
			Collection<SubscriptionManager> managers = this.subscriptionManagers.values();
			boolean removed = false;
			for (SubscriptionManager manager : managers) {
				removed = (removed || manager.removeTarget((TargetEndpoint) endpoint));
			}
			if (removed) {
				return endpoint;
			}
		}
		return null;
	}

	public MessageEndpoint lookupEndpoint(String endpointName) {
		return this.endpointRegistry.lookupEndpoint(endpointName);
	}

	public Set<String> getEndpointNames() {
		return this.endpointRegistry.getEndpointNames();
	}

	private void activateEndpoints() {
		Set<String> endpointNames = this.endpointRegistry.getEndpointNames();
		for (String name : endpointNames) {
			MessageEndpoint endpoint = this.endpointRegistry.lookupEndpoint(name);
			if (endpoint != null) {
				this.activateEndpoint(endpoint);
			}
		}
	}

	private void activateEndpoint(MessageEndpoint endpoint) {
		if (endpoint instanceof TargetEndpoint) {
			this.activateTargetEndpoint((TargetEndpoint) endpoint);
		}
	}

	private void activateTargetEndpoint(TargetEndpoint endpoint) {
		Subscription subscription = endpoint.getSubscription();
		if (subscription == null) {
			throw new ConfigurationException("Unable to register endpoint '" + endpoint
					+ "'. No subscription information is available.");
		}
		MessageChannel channel = subscription.getChannel();
		if (channel == null) {
			String channelName = subscription.getChannelName();
			if (channelName == null) {
				throw new ConfigurationException("endpoint '" + endpoint
						+ "' must provide either 'channel' or 'channelName' in its subscription metadata");
			}
			channel = this.lookupChannel(channelName);
			if (channel == null) {
				if (!this.autoCreateChannels) {
					throw new ConfigurationException("Cannot activate subscription, unknown channel '" + channelName
							+ "'. Consider enabling the 'autoCreateChannels' option for the message bus.");
				}
				if (this.logger.isInfoEnabled()) {
					logger.info("auto-creating channel '" + channelName + "'");
				}
				channel = channelFactory.getChannel(null, null);
				this.registerChannel(channelName, channel);
			}
		}
		if (endpoint instanceof HandlerEndpoint) {
			HandlerEndpoint handlerEndpoint = (HandlerEndpoint) endpoint;
			String outputChannelName = handlerEndpoint.getOutputChannelName();
			if (outputChannelName != null && this.lookupChannel(outputChannelName) == null) {
				if (!this.autoCreateChannels) {
					throw new ConfigurationException("Unknown channel '" + outputChannelName
							+ "' configured as output channel for endpoint '" + endpoint
							+ "'. Consider enabling the 'autoCreateChannels' option for the message bus.");
				}
				this.registerChannel(outputChannelName, new QueueChannel());
			}
		}
		if (!endpoint.hasErrorHandler() && this.getErrorChannel() != null && !this.getErrorChannel().equals(channel)) {
			endpoint.setErrorHandler(new MessagePublishingErrorHandler(this.getErrorChannel()));
		}
		endpoint.afterPropertiesSet();
		this.activateSubscription(channel, endpoint, subscription.getSchedule());
		if (logger.isInfoEnabled()) {
			logger
					.info("activated subscription to channel '" + channel.getName() + "' for endpoint '" + endpoint
							+ "'");
		}
	}

	private void registerSourceEndpoint(String name, SourceEndpoint endpoint) {
		if (!this.initialized) {
			this.initialize();
		}
		this.taskScheduler.schedule(endpoint);
		if (endpoint instanceof Lifecycle) {
			this.lifecycleEndpoints.add((Lifecycle) endpoint);
			if (this.isRunning()) {
				((Lifecycle) endpoint).start();
			}
		}
		if (logger.isInfoEnabled()) {
			logger.info("registered source adapter '" + name + "'");
		}
	}

	private void registerGateway(String name, MessagingGateway gateway) {
		if (gateway instanceof Lifecycle) {
			this.lifecycleEndpoints.add((Lifecycle) gateway);
			if (this.isRunning()) {
				((Lifecycle) gateway).start();
			}
		}
		if (logger.isInfoEnabled()) {
			logger.info("registered gateway '" + name + "'");
		}
	}

	private void activateSubscription(MessageChannel channel, Target target, Schedule schedule) {
		SubscriptionManager manager = this.subscriptionManagers.get(channel);
		if (manager == null) {
			if (logger.isWarnEnabled()) {
				logger.warn("no subscription manager available for channel '" + channel
						+ "', be sure to register the channel");
			}
			return;
		}
		manager.addTarget(target, schedule);
		if (this.isRunning() && !manager.isRunning()) {
			manager.start();
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
		this.interceptors.preStart();
		this.starting = true;
		synchronized (this.lifecycleMonitor) {
			this.activateEndpoints();
			this.taskScheduler.start();
			for (SubscriptionManager manager : this.subscriptionManagers.values()) {
				manager.start();
				if (logger.isInfoEnabled()) {
					logger.info("started subscription manager '" + manager + "'");
				}
			}
			for (Lifecycle endpoint : this.lifecycleEndpoints) {
				endpoint.start();
				if (logger.isInfoEnabled()) {
					logger.info("started endpoint '" + endpoint + "'");
				}
			}
		}
		this.running = true;
		this.starting = false;
		this.interceptors.postStart();
		if (logger.isInfoEnabled()) {
			logger.info("message bus started");
		}
	}

	public void stop() {
		if (!this.isRunning()) {
			return;
		}
		this.interceptors.preStop();
		synchronized (this.lifecycleMonitor) {
			this.running = false;
			this.taskScheduler.stop();
			for (Lifecycle endpoint : this.lifecycleEndpoints) {
				endpoint.stop();
				if (logger.isInfoEnabled()) {
					logger.info("stopped endpoint '" + endpoint + "'");
				}
			}
			for (SubscriptionManager manager : this.subscriptionManagers.values()) {
				manager.stop();
				if (logger.isInfoEnabled()) {
					logger.info("stopped subscription manager '" + manager + "'");
				}
			}
		}
		this.interceptors.postStop();
		if (logger.isInfoEnabled()) {
			logger.info("message bus stopped");
		}
	}

	public void onApplicationEvent(ApplicationEvent event) {
		if (event instanceof ContextRefreshedEvent) {
			ApplicationContext context = ((ContextRefreshedEvent) event).getApplicationContext();
			this.registerEndpoints(context);
			this.registerGateways(context);
			if (this.configureAsyncEventMulticaster) {
				this.initialize();
				this.doConfigureAsyncEventMulticaster(context);
			}
			if (this.autoStartup) {
				this.start();
			}
		}
	}

	private void doConfigureAsyncEventMulticaster(ApplicationContext context) {
		String multicasterBeanName = AbstractApplicationContext.APPLICATION_EVENT_MULTICASTER_BEAN_NAME;
		if (context.containsBean(multicasterBeanName)) {
			ApplicationEventMulticaster multicaster = (ApplicationEventMulticaster) context
					.getBean(multicasterBeanName);
			if (multicaster instanceof SimpleApplicationEventMulticaster) {
				((SimpleApplicationEventMulticaster) multicaster).setTaskExecutor(this.taskScheduler);
			}
		}
	}

	public void addInterceptor(MessageBusInterceptor interceptor) {
		this.interceptors.add(interceptor);
	}

	public void removeInterceptor(MessageBusInterceptor interceptor) {
		this.interceptors.remove(interceptor);
	}

	public void setInterceptors(List<MessageBusInterceptor> interceptor) {
		this.interceptors.set(interceptor);
	}

	/*
	 * Wrapper class for the interceptor list
	 */
	private class MessageBusInterceptorsList {

		private CopyOnWriteArrayList<MessageBusInterceptor> messageBusInterceptors = new CopyOnWriteArrayList<MessageBusInterceptor>();

		public void set(List<MessageBusInterceptor> interceptors) {
			this.messageBusInterceptors.clear();
			this.messageBusInterceptors.addAll(interceptors);
		}

		public void add(MessageBusInterceptor interceptor) {
			this.messageBusInterceptors.add(interceptor);
		}

		public void remove(MessageBusInterceptor interceptor) {
			this.messageBusInterceptors.remove(interceptor);
		}

		public void preStart() {
			for (MessageBusInterceptor messageBusInterceptor : messageBusInterceptors) {
				messageBusInterceptor.preStart(MessageBus.this);
			}
		}

		public void postStart() {
			for (MessageBusInterceptor messageBusInterceptor : messageBusInterceptors) {
				messageBusInterceptor.postStart(MessageBus.this);
			}
		}

		public void preStop() {
			for (MessageBusInterceptor messageBusInterceptor : messageBusInterceptors) {
				messageBusInterceptor.preStop(MessageBus.this);
			}
		}

		public void postStop() {
			for (MessageBusInterceptor messageBusInterceptor : messageBusInterceptors) {
				messageBusInterceptor.postStop(MessageBus.this);
			}
		}
	}

}
