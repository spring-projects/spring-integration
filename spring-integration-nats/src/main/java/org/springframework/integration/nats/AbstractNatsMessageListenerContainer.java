/*
 * Copyright 2016-2024 the original author or authors.
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


package org.springframework.integration.nats;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import io.nats.client.MessageHandler;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.SmartLifecycle;
import org.springframework.lang.NonNull;

/**
 * Base class for Nats Listener container
 *
 * @author Viktor Rohlenko
 * @author Vennila Pazhamalai
 * @author Vivek Duraisamy
 * @since 6.4.x
 *
 * @see <a
 *     href="https://rohlenko.github.io/spring-integration-nats-site/gws-spring-integration-nats/index.html#stakeholders">See
 *     all stakeholders and contact</a>
 */
public abstract class AbstractNatsMessageListenerContainer
		implements SmartLifecycle, BeanNameAware, ApplicationContextAware {

	protected final Object lifecycleMonitor = new Object();
	@NonNull
	protected final NatsConsumerFactory natsConsumerFactory;
	@NonNull
	protected final NatsMessageDeliveryMode natsMessageDeliveryMode;
	protected MessageHandler messageHandler;
	private String beanName;
	private volatile boolean running = false;
	private boolean autoStartup = true;
	private ApplicationContext applicationContext;

	/**
	 * Construct an instance with the consumerFactory and default delivery mode PULL.
	 *
	 * @param pNatsConsumerFactory the NatsConsumerFactory bean with required information to create
	 *     subscription and start polling for message
	 */
	public AbstractNatsMessageListenerContainer(
			@NonNull final NatsConsumerFactory pNatsConsumerFactory) {
		this.natsConsumerFactory = pNatsConsumerFactory;
		this.natsMessageDeliveryMode = NatsMessageDeliveryMode.PULL;
	}

	/**
	 * Construct an instance with the consumerFactory and delivery mode
	 *
	 * @param pNatsConsumerFactory NatsConsumerFactory bean with required information to create
	 *     subscription and start polling for message
	 * @param pNatsMessageDeliveryMode option to provide the message delivery mode
	 */
	public AbstractNatsMessageListenerContainer(
			@NonNull final NatsConsumerFactory pNatsConsumerFactory,
			@NonNull final NatsMessageDeliveryMode pNatsMessageDeliveryMode) {
		this.natsConsumerFactory = pNatsConsumerFactory;
		this.natsMessageDeliveryMode = pNatsMessageDeliveryMode;
	}

	protected abstract void doStart();

	protected abstract void doStop(Runnable callback);

	@Override
	public void start() {
		synchronized (this.lifecycleMonitor) {
			if (!isRunning()) {
				doStart();
			}
		}
	}

	@Override
	public void stop() {
		stop(true);
	}

	public final void stop(final boolean wait) {
		synchronized (this.lifecycleMonitor) {
			if (isRunning()) {
				if (wait) {
					final CountDownLatch latch = new CountDownLatch(1);
					doStop(latch::countDown);
					try {
						latch.await(5000, TimeUnit.MILLISECONDS); // NOSONAR
					}
					catch (
							@SuppressWarnings("unused") final InterruptedException e) {
						Thread.currentThread().interrupt();
					}
				}
				else {
					doStop(() -> {
					});
				}
			}
		}
	}

	@Override
	public void stop(final Runnable callback) {
		synchronized (this.lifecycleMonitor) {
			if (isRunning()) {
				doStop(callback);
			}
			else {
				callback.run();
			}
		}
	}

	@Override
	public boolean isRunning() {
		return this.running;
	}

	protected void setRunning(final boolean pRunning) {
		this.running = pRunning;
	}

	public String getBeanName() {
		return this.beanName;
	}

	@Override
	public void setBeanName(final String name) {
		this.beanName = name;
	}

	@Override
	public boolean isAutoStartup() {
		return this.autoStartup;
	}

	public void setAutoStartup(final boolean pAutoStartup) {
		this.autoStartup = pAutoStartup;
	}

	protected ApplicationContext getApplicationContext() {
		return this.applicationContext;
	}

	@Override
	public void setApplicationContext(final ApplicationContext pApplicationContext)
			throws BeansException {
		this.applicationContext = pApplicationContext;
	}

	/**
	 * Sets the messageHandler reference
	 *
	 * @param pMessageHandler the messageHandler
	 */
	public void setMessageHandler(@NonNull final MessageHandler pMessageHandler) {
		this.messageHandler = pMessageHandler;
	}
}
