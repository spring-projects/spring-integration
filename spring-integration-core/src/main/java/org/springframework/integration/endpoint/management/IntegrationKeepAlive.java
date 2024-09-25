/*
 * Copyright 2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.endpoint.management;

import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.context.SmartLifecycle;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.context.IntegrationProperties;
import org.springframework.integration.endpoint.AbstractPollingEndpoint;
import org.springframework.scheduling.TaskScheduler;

/**
 * The component to keep an application alive when there are no non-daemon threads.
 * Some application might just not rely on the loops in specific threads for their logic.
 * Or target protocol to integrate with communicates via daemon threads.
 * <p>
 * A bean for this class is registered automatically by Spring Integration infrastructure.
 * It is started by application context for a blocked keep-alive dedicated thread
 * only if there is no {@link AbstractPollingEndpoint} beans in the application context
 * or {@link TaskScheduler} is configured for daemon (or virtual) threads.
 * <p>
 * Can be stopped (or started respectively) manually after injection into some target service if found redundant.
 * <p>
 * The {@link IntegrationProperties#KEEP_ALIVE} integration global
 * property can be set to {@code false} to disable this component regardless of the application logic.
 *
 * @author Artem Bilan
 *
 * @since 6.4
 */
public class IntegrationKeepAlive implements SmartLifecycle, SmartInitializingSingleton, BeanFactoryAware {

	private static final Log LOG = LogFactory.getLog(IntegrationKeepAlive.class);

	private final AtomicBoolean running = new AtomicBoolean();

	private BeanFactory beanFactory;

	private boolean autoStartup;

	private volatile Thread keepAliveThread;

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@Override
	public void afterSingletonsInstantiated() {
		IntegrationProperties integrationProperties = IntegrationContextUtils.getIntegrationProperties(this.beanFactory);
		this.autoStartup =
				integrationProperties.isKeepAlive()
						&& (isTaskSchedulerDaemon() || !isAbstractPollingEndpointPresent());
	}

	private boolean isTaskSchedulerDaemon() {
		TaskScheduler taskScheduler = IntegrationContextUtils.getTaskScheduler(this.beanFactory);
		AtomicBoolean isDaemon = new AtomicBoolean();
		CountDownLatch checkDaemonThreadLatch = new CountDownLatch(1);
		taskScheduler.schedule(() -> {
			isDaemon.set(Thread.currentThread().isDaemon());
			checkDaemonThreadLatch.countDown();
		}, Instant.now());

		boolean logWarning = false;
		try {
			if (!checkDaemonThreadLatch.await(10, TimeUnit.SECONDS)) {
				logWarning = true;
			}
		}
		catch (InterruptedException ex) {
			logWarning = true;
		}
		if (logWarning) {
			LOG.warn("The 'IntegrationKeepAlive' cannot check a 'TaskScheduler' daemon threads status. " +
					"Falling back to 'keep-alive'");
		}
		return isDaemon.get();
	}

	private boolean isAbstractPollingEndpointPresent() {
		return this.beanFactory.getBeanProvider(AbstractPollingEndpoint.class)
				.stream()
				.findAny()
				.isPresent();
	}

	@Override
	public boolean isAutoStartup() {
		return this.autoStartup;
	}

	@Override
	public void start() {
		if (this.running.compareAndSet(false, true)) {
			this.keepAliveThread =
					new Thread(() -> {
						while (true) {
							try {
								Thread.sleep(Long.MAX_VALUE);
							}
							catch (InterruptedException ex) {
								break;
							}
						}
					});
			this.keepAliveThread.setDaemon(false);
			this.keepAliveThread.setName("spring-integration-keep-alive");
			this.keepAliveThread.start();
		}
	}

	@Override
	public void stop() {
		if (this.running.compareAndSet(true, false)) {
			this.keepAliveThread.interrupt();
		}
	}

	@Override
	public boolean isRunning() {
		return this.running.get();
	}

}
