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
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CopyOnWriteArraySet;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.generic.GenericBeanFactoryAccessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.Lifecycle;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.integration.channel.ChannelRegistryAware;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.MessagePublishingErrorHandler;
import org.springframework.integration.endpoint.MessageEndpoint;
import org.springframework.integration.endpoint.MessagingGateway;
import org.springframework.integration.scheduling.SimpleTaskScheduler;
import org.springframework.integration.scheduling.TaskScheduler;
import org.springframework.integration.scheduling.TaskSchedulerAware;
import org.springframework.util.Assert;

/**
 * The messaging bus. Serves as a registry for channels and endpoints, manages their lifecycle,
 * and activates subscriptions.
 * 
 * @author Mark Fisher
 * @author Marius Bogoevici
 */
public class DefaultMessageBus implements MessageBus, ApplicationContextAware, ApplicationListener, DisposableBean {

	private final Log logger = LogFactory.getLog(this.getClass());

	private final Set<MessageEndpoint> endpoints = new CopyOnWriteArraySet<MessageEndpoint>();

	private final MessageBusInterceptorsList interceptors = new MessageBusInterceptorsList();

	private final Set<Lifecycle> lifecycleGateways = new CopyOnWriteArraySet<Lifecycle>();

	private volatile TaskScheduler taskScheduler;

	private volatile ApplicationContext applicationContext;

	private volatile boolean autoStartup = true;

	private volatile boolean initialized;

	private volatile boolean running;

	private final Object lifecycleMonitor = new Object();


	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		Assert.notNull(applicationContext, "'applicationContext' must not be null");
		Assert.state(!(applicationContext.getBeanNamesForType(this.getClass()).length > 1),
				"Only one instance of '" + this.getClass().getSimpleName() + "' is allowed per ApplicationContext.");
		this.applicationContext = applicationContext;
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
			if (this.initialized) {
				return;
			}
			Assert.notNull(this.applicationContext, "ApplicationContext must not be null");
			Assert.notNull(this.taskScheduler, "TaskScheduler must not be null");
			this.initialized = true;
		}
	}

	public MessageChannel getErrorChannel() {
		return this.lookupChannel(ERROR_CHANNEL_NAME);
	}

	public MessageChannel lookupChannel(String channelName) {
		Assert.notNull(this.applicationContext, "ApplicationContext must not be null");
		if (this.applicationContext.containsBean(channelName)) {
			Object bean = this.applicationContext.getBean(channelName);
			if (bean instanceof MessageChannel) {
				return (MessageChannel) bean;
			}
		}
		return null;
	}

	public void registerEndpoint(MessageEndpoint endpoint) {
		Assert.notNull(endpoint, "'endpoint' must not be null");
		if (!this.endpoints.contains(endpoint)) {
			this.endpoints.add(endpoint);
		}
		if (this.isRunning()) {
			this.activateEndpoint(endpoint);
		}
		if (logger.isInfoEnabled()) {
			logger.info("registered endpoint '" + endpoint + "'");
		}
	}

	public MessageEndpoint lookupEndpoint(String endpointName) {
		if (this.applicationContext.containsBean(endpointName)) {
			Object bean = this.applicationContext.getBean(endpointName);
			if (bean instanceof MessageEndpoint) {
				return (MessageEndpoint) bean;
			}
		}
		return null;
	}

	private Collection<MessageEndpoint> getEndpoints() {
		GenericBeanFactoryAccessor accessor = new GenericBeanFactoryAccessor(this.applicationContext);
		this.endpoints.addAll(accessor.getBeansOfType(MessageEndpoint.class).values());
		return this.endpoints;
	}

	private void activateEndpoints() {
		for (MessageEndpoint endpoint : this.getEndpoints()) {
			if (endpoint != null) {
				this.activateEndpoint(endpoint);
			}
		}
	}

	private void deactivateEndpoints() {
		for (MessageEndpoint endpoint : this.getEndpoints()) {
			if (endpoint != null) {
				this.deactivateEndpoint(endpoint);
			}
		}
	}

	private void activateEndpoint(MessageEndpoint endpoint) {
		Assert.notNull(endpoint, "'endpoint' must not be null");
		if (endpoint instanceof ChannelRegistryAware) {
			((ChannelRegistryAware) endpoint).setChannelRegistry(this);
		}
		if (endpoint instanceof TaskSchedulerAware) {
			((TaskSchedulerAware) endpoint).setTaskScheduler(this.taskScheduler);
		}
		if (endpoint instanceof Lifecycle) {
			((Lifecycle) endpoint).start();
		}
		if (logger.isInfoEnabled()) {
			logger.info("activated endpoint '" + endpoint + "'");
		}
	}

	public void deactivateEndpoint(MessageEndpoint endpoint) {
		Assert.notNull(endpoint, "'endpoint' must not be null");
		if (endpoint instanceof Lifecycle) {
			((Lifecycle) endpoint).stop();
			if (this.logger.isInfoEnabled()) {
				logger.info("deactivated endpoint '" + endpoint + "'");
			}
		}
	}

	// TODO: once gateways are endpoints, remove this 
	private void registerGateway(String name, MessagingGateway gateway) {
		if (gateway instanceof Lifecycle) {
			this.lifecycleGateways.add((Lifecycle) gateway);
			if (this.isRunning()) {
				((Lifecycle) gateway).start();
			}
		}
		if (logger.isInfoEnabled()) {
			logger.info("registered gateway '" + name + "'");
		}
	}

	// Lifecycle implementation

	public boolean isRunning() {
		synchronized (this.lifecycleMonitor) {
			return this.running;
		}
	}

	public void start() {
		if (!this.initialized) {
			this.initialize();
		}
		if (this.running) {
			return;
		}
		this.interceptors.preStart();
		synchronized (this.lifecycleMonitor) {
			this.activateEndpoints();
			for (Lifecycle gateway : this.lifecycleGateways) {
				gateway.start();
			}
			if (this.taskScheduler instanceof SimpleTaskScheduler) {
				((SimpleTaskScheduler) this.taskScheduler).setErrorHandler(new MessagePublishingErrorHandler(this.getErrorChannel()));
			}
			this.taskScheduler.start();
		}
		this.running = true;
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
			this.deactivateEndpoints();
			for (Lifecycle gateway : this.lifecycleGateways) {
				gateway.stop();
			}
			this.running = false;
			this.taskScheduler.stop();
		}
		this.interceptors.postStop();
		if (logger.isInfoEnabled()) {
			logger.info("message bus stopped");
		}
	}

	public void destroy() throws Exception {
		this.stop();
		if (this.taskScheduler instanceof DisposableBean) {
			((DisposableBean) this.taskScheduler).destroy();
		}
	}

	public void onApplicationEvent(ApplicationEvent event) {
		if (event instanceof ContextRefreshedEvent) {
			ApplicationContext context = ((ContextRefreshedEvent) event).getApplicationContext();
			this.registerGateways(context);
			if (this.autoStartup) {
				this.start();
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
