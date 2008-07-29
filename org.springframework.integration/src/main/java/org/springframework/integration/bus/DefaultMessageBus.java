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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor.CallerRunsPolicy;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
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
import org.springframework.integration.channel.ChannelRegistry;
import org.springframework.integration.channel.ChannelRegistryAware;
import org.springframework.integration.channel.DefaultChannelRegistry;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.factory.ChannelFactory;
import org.springframework.integration.channel.factory.QueueChannelFactory;
import org.springframework.integration.endpoint.AbstractEndpoint;
import org.springframework.integration.endpoint.DefaultEndpointRegistry;
import org.springframework.integration.endpoint.EndpointRegistry;
import org.springframework.integration.endpoint.EndpointTrigger;
import org.springframework.integration.endpoint.HandlerEndpoint;
import org.springframework.integration.endpoint.MessageEndpoint;
import org.springframework.integration.endpoint.MessagingGateway;
import org.springframework.integration.endpoint.TargetEndpoint;
import org.springframework.integration.handler.MessageHandler;
import org.springframework.integration.message.MessageSource;
import org.springframework.integration.message.MessageTarget;
import org.springframework.integration.message.Subscribable;
import org.springframework.integration.scheduling.MessagePublishingErrorHandler;
import org.springframework.integration.scheduling.PollingSchedule;
import org.springframework.integration.scheduling.Schedule;
import org.springframework.integration.scheduling.SimpleTaskScheduler;
import org.springframework.integration.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.CustomizableThreadFactory;
import org.springframework.util.Assert;

/**
 * The messaging bus. Serves as a registry for channels and endpoints, manages their lifecycle,
 * and activates subscriptions.
 * 
 * @author Mark Fisher
 * @author Marius Bogoevici
 */
public class DefaultMessageBus implements MessageBus, ApplicationContextAware, ApplicationListener {

	private static final int DEFAULT_DISPATCHER_POOL_SIZE = 10;

	private final Log logger = LogFactory.getLog(this.getClass());

	private volatile ChannelFactory channelFactory = new QueueChannelFactory();

	private final ChannelRegistry channelRegistry = new DefaultChannelRegistry();

	private final EndpointRegistry endpointRegistry = new DefaultEndpointRegistry();

	private final Set<EndpointTrigger> endpointTriggers = new CopyOnWriteArraySet<EndpointTrigger>();

	private volatile Schedule defaultPollerSchedule = new PollingSchedule(0);

	private final List<Lifecycle> lifecycleEndpoints = new CopyOnWriteArrayList<Lifecycle>();

	private final MessageBusInterceptorsList interceptors = new MessageBusInterceptorsList();

	private volatile TaskScheduler taskScheduler;

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
	 * Set the {@link TaskScheduler} to use for scheduling message dispatchers.
	 */
	public void setTaskScheduler(TaskScheduler taskScheduler) {
		this.taskScheduler = taskScheduler;
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
			String channelName = entry.getKey();
			MessageChannel previousChannel = this.lookupChannel(channelName);
			if (previousChannel == null) {
				this.registerChannel(channelName, entry.getValue());
			}
			else if (!previousChannel.equals(entry.getValue())) {
				throw new ConfigurationException("A different channel instance has already "
						+ "been registered with the name '" + channelName + "'.");
			}
		}
	}

	@SuppressWarnings("unchecked")
	private void registerEndpoints(ApplicationContext context) {
		Map<String, MessageEndpoint> endpointBeans = (Map<String, MessageEndpoint>) context
				.getBeansOfType(MessageEndpoint.class);
		for (Map.Entry<String, MessageEndpoint> entry : endpointBeans.entrySet()) {
			this.registerEndpoint(entry.getValue());
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
			if (this.taskScheduler == null) {
				ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(DEFAULT_DISPATCHER_POOL_SIZE);
				executor.setThreadFactory(new CustomizableThreadFactory("message-bus-"));
				executor.setRejectedExecutionHandler(new CallerRunsPolicy());
				this.taskScheduler = new SimpleTaskScheduler(executor);
			}
			if (this.getErrorChannel() == null) {
				this.setErrorChannel(new DefaultErrorChannel());
			}
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
		this.channelRegistry.registerChannel(name, channel);
		if (logger.isInfoEnabled()) {
			logger.info("registered channel '" + name + "'");
		}
	}

	public MessageChannel unregisterChannel(String name) {
		return this.channelRegistry.unregisterChannel(name);
	}

	public void registerHandler(String name, MessageHandler handler, Object input, Schedule schedule) {
		Assert.notNull(handler, "'handler' must not be null");
		HandlerEndpoint endpoint = new HandlerEndpoint(handler);
		this.configureEndpoint(endpoint, name, input, schedule);
		this.registerEndpoint(endpoint);
	}

	public void registerTarget(String name, MessageTarget target, Object input, Schedule schedule) {
		Assert.notNull(target, "'target' must not be null");
		TargetEndpoint endpoint = new TargetEndpoint(target);
		this.configureEndpoint(endpoint, name, input, schedule);
		this.registerEndpoint(endpoint);
	}

	private void configureEndpoint(AbstractEndpoint endpoint, String name, Object input, Schedule schedule) {
		endpoint.setName(name);
		if (input instanceof MessageChannel) {
			endpoint.setSource((MessageChannel) input);
		}
		else if (input instanceof String) {
			endpoint.setInputChannelName((String) input);
		}
		else {
			throw new ConfigurationException("'input' must be a MessageChannel or String");
		}
		endpoint.setSchedule(schedule);
	}

	public void registerEndpoint(MessageEndpoint endpoint) {
		if (!this.initialized) {
			this.initialize();
		}
		if (endpoint instanceof ChannelRegistryAware) {
			((ChannelRegistryAware) endpoint).setChannelRegistry(this.channelRegistry);
		}
		this.endpointRegistry.registerEndpoint(endpoint);
		if (this.isRunning()) {
			this.activateEndpoint(endpoint);
		}
		if (logger.isInfoEnabled()) {
			logger.info("registered endpoint '" + endpoint + "'");
		}
	}

	public MessageEndpoint unregisterEndpoint(String name) {
		MessageEndpoint endpoint = this.endpointRegistry.unregisterEndpoint(name);
		if (endpoint == null) {
			return null;
		}
		this.deactivateEndpoint(endpoint);
		return endpoint;
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
		Assert.notNull(endpoint, "'endpoint' must not be null");
		if (endpoint.getTarget() == null) {
			this.lookupOrCreateChannel(endpoint.getOutputChannelName());
		}
		MessageSource<?> source = endpoint.getSource();
		if (source == null) {
			source = this.lookupOrCreateChannel(endpoint.getInputChannelName());
			if (source != null) {
				endpoint.setSource(source);
			}
		}
		if (source != null && source instanceof Subscribable) {
			((Subscribable) source).subscribe(endpoint);
			if (logger.isInfoEnabled()) {
				logger.info("activated subscription to channel '"
						+ source + "' for endpoint '" + endpoint + "'");
			}
			return;
		}
		Schedule schedule = endpoint.getSchedule();
		EndpointTrigger trigger = new EndpointTrigger(schedule != null ? schedule : this.defaultPollerSchedule);
		trigger.addTarget(endpoint);
		if (this.endpointTriggers.add(trigger)) {
			this.taskScheduler.schedule(trigger);
		}
	}

	private MessageChannel lookupOrCreateChannel(String channelName) {
		if (channelName == null) {
			return null;
		}
		MessageChannel channel = this.lookupChannel(channelName);
		if (channel == null) {
			if (!this.autoCreateChannels) {
				throw new ConfigurationException("Cannot activate endpoint, unknown channel '" + channelName
						+ "'. Consider enabling the 'autoCreateChannels' option for the message bus.");
			}
			if (this.logger.isInfoEnabled()) {
				logger.info("auto-creating channel '" + channelName + "'");
			}
			channel = channelFactory.getChannel(channelName, null);
			this.registerChannel(channelName, channel);
		}
		return channel;
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

	public void deactivateEndpoint(MessageEndpoint endpoint) {
		Assert.notNull(endpoint, "'endpoint' must not be null");
		for (EndpointTrigger trigger : this.endpointTriggers) {
			boolean removed = trigger.removeTarget(endpoint);
			if (removed && this.logger.isInfoEnabled()) {
				logger.info("removed endpoint '" + endpoint + "' from dispatcher");
			}
		}
		if (endpoint instanceof Lifecycle) {
			((Lifecycle) endpoint).stop();
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
			this.taskScheduler.setErrorHandler(new MessagePublishingErrorHandler(this.getErrorChannel()));
			this.taskScheduler.start();
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
		}
		this.interceptors.postStop();
		if (logger.isInfoEnabled()) {
			logger.info("message bus stopped");
		}
	}

	public void destroy() throws Exception {
		if (this.taskScheduler instanceof DisposableBean) {
			((DisposableBean) this.taskScheduler).destroy();
		}
	}

	public void onApplicationEvent(ApplicationEvent event) {
		if (event instanceof ContextRefreshedEvent) {
			ApplicationContext context = ((ContextRefreshedEvent) event).getApplicationContext();
			this.registerChannels(context);
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
				messageBusInterceptor.preStart(DefaultMessageBus.this);
			}
		}

		public void postStart() {
			for (MessageBusInterceptor messageBusInterceptor : messageBusInterceptors) {
				messageBusInterceptor.postStart(DefaultMessageBus.this);
			}
		}

		public void preStop() {
			for (MessageBusInterceptor messageBusInterceptor : messageBusInterceptors) {
				messageBusInterceptor.preStop(DefaultMessageBus.this);
			}
		}

		public void postStop() {
			for (MessageBusInterceptor messageBusInterceptor : messageBusInterceptors) {
				messageBusInterceptor.postStop(DefaultMessageBus.this);
			}
		}
	}

}
