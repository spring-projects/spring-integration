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
import java.util.Set;
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
import org.springframework.integration.channel.BeanFactoryChannelResolver;
import org.springframework.integration.channel.ChannelResolver;
import org.springframework.integration.core.MessageChannel;
import org.springframework.integration.endpoint.MessageEndpoint;
import org.springframework.integration.scheduling.TaskScheduler;
import org.springframework.integration.scheduling.TaskSchedulerAware;
import org.springframework.util.Assert;

/**
 * Spring Integration's standard Message Bus implementation. Serves as a
 * registry for Messages Endpoints. Manages their lifecycle, activates
 * subscriptions, and schedules pollers by delegating to a
 * {@link TaskScheduler}. Retrieves MessageChannels from the
 * ApplicationContext based on bean name.
 * 
 * @author Mark Fisher
 * @author Marius Bogoevici
 */
public class ApplicationContextMessageBus implements Lifecycle, ChannelResolver, ApplicationContextAware, ApplicationListener, DisposableBean {

	public static final String ERROR_CHANNEL_BEAN_NAME = "errorChannel";


	private final Log logger = LogFactory.getLog(this.getClass());

	private final Set<MessageEndpoint> endpoints = new CopyOnWriteArraySet<MessageEndpoint>();

	private volatile TaskScheduler taskScheduler;

	private volatile ApplicationContext applicationContext;

	private volatile ChannelResolver channelResolver;

	private volatile boolean autoStartup = true;

	private volatile boolean running;

	private final Object lifecycleMonitor = new Object();


	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		Assert.notNull(applicationContext, "'applicationContext' must not be null");
		Assert.state(!(applicationContext.getBeanNamesForType(this.getClass()).length > 1),
				"Only one instance of '" + this.getClass().getSimpleName() + "' is allowed per ApplicationContext.");
		this.applicationContext = applicationContext;
		this.channelResolver = new BeanFactoryChannelResolver(applicationContext);
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

	public MessageChannel resolveChannelName(String channelName) {
		return this.channelResolver.resolveChannelName(channelName);
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

	private void deactivateEndpoint(MessageEndpoint endpoint) {
		Assert.notNull(endpoint, "'endpoint' must not be null");
		if (endpoint instanceof Lifecycle) {
			((Lifecycle) endpoint).stop();
			if (this.logger.isInfoEnabled()) {
				logger.info("deactivated endpoint '" + endpoint + "'");
			}
		}
	}

	// Lifecycle implementation

	public boolean isRunning() {
		synchronized (this.lifecycleMonitor) {
			return this.running;
		}
	}

	public void start() {
		if (this.running) {
			return;
		}
		Assert.notNull(this.applicationContext, "ApplicationContext must not be null");
		Assert.notNull(this.taskScheduler, "TaskScheduler must not be null");
		synchronized (this.lifecycleMonitor) {
			this.activateEndpoints();
			this.taskScheduler.start();
		}
		this.running = true;
		this.applicationContext.publishEvent(new MessageBusStartedEvent(this));
		if (logger.isInfoEnabled()) {
			logger.info("message bus started");
		}
	}

	public void stop() {
		if (!this.isRunning()) {
			return;
		}
		synchronized (this.lifecycleMonitor) {
			this.deactivateEndpoints();
			this.running = false;
			this.taskScheduler.stop();
		}
		this.applicationContext.publishEvent(new MessageBusStoppedEvent(this));
		if (logger.isInfoEnabled()) {
			logger.info("message bus stopped");
		}
	}

	// ApplicationListener implementation

	public void onApplicationEvent(ApplicationEvent event) {
		if (event instanceof ContextRefreshedEvent && this.autoStartup) {
			this.start();
		}
	}

	// DisposableBean implementation

	public void destroy() throws Exception {
		this.stop();
		if (this.taskScheduler instanceof DisposableBean) {
			((DisposableBean) this.taskScheduler).destroy();
		}
	}

}
