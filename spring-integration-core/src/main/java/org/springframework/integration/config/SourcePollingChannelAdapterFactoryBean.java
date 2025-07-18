/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.config;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.core.BeanFactoryMessageChannelDestinationResolver;
import org.springframework.messaging.core.DestinationResolver;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * FactoryBean for creating a SourcePollingChannelAdapter instance.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 * @author Christian Tzolov
 */
public class SourcePollingChannelAdapterFactoryBean implements FactoryBean<SourcePollingChannelAdapter>,
		BeanFactoryAware, BeanNameAware, BeanClassLoaderAware, InitializingBean, SmartLifecycle, DisposableBean {

	private final Lock initializationMonitor = new ReentrantLock();

	@SuppressWarnings("NullAway.Init")
	private MessageSource<?> source;

	@Nullable
	private MessageChannel outputChannel;

	@Nullable
	private String outputChannelName;

	@Nullable
	private PollerMetadata pollerMetadata;

	@Nullable
	private Boolean autoStartup;

	private int phase = Integer.MAX_VALUE / 2;

	@Nullable
	private Long sendTimeout;

	@SuppressWarnings("NullAway.Init")
	private String beanName;

	@SuppressWarnings("NullAway.Init")
	private ConfigurableBeanFactory beanFactory;

	@SuppressWarnings("NullAway.Init")
	private ClassLoader beanClassLoader;

	@SuppressWarnings("NullAway.Init")
	private DestinationResolver<MessageChannel> channelResolver;

	@Nullable
	private String role;

	@Nullable
	private TaskScheduler taskScheduler;

	@SuppressWarnings("NullAway.Init")
	private volatile SourcePollingChannelAdapter adapter;

	private volatile boolean initialized;

	public void setSource(MessageSource<?> source) {
		this.source = source;
	}

	public void setSendTimeout(long sendTimeout) {
		this.sendTimeout = sendTimeout;
	}

	public void setOutputChannel(MessageChannel outputChannel) {
		this.outputChannel = outputChannel;
	}

	public void setOutputChannelName(String outputChannelName) {
		this.outputChannelName = outputChannelName;
	}

	public void setPollerMetadata(PollerMetadata pollerMetadata) {
		this.pollerMetadata = pollerMetadata;
	}

	public void setAutoStartup(Boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	public void setPhase(int phase) {
		this.phase = phase;
	}

	public void setRole(String role) {
		this.role = role;
	}

	/**
	 * Set a {@link TaskScheduler} for polling tasks.
	 * @param taskScheduler the {@link TaskScheduler} for polling tasks.
	 * @since 6.4
	 */
	public void setTaskScheduler(TaskScheduler taskScheduler) {
		this.taskScheduler = taskScheduler;
	}

	/**
	 * Specify the {@link DestinationResolver} strategy to use.
	 * The default is a BeanFactoryChannelResolver.
	 * @param channelResolver The channel resolver.
	 * @since 4.1.3
	 */
	public void setChannelResolver(DestinationResolver<MessageChannel> channelResolver) {
		Assert.notNull(channelResolver, "'channelResolver' must not be null");
		this.channelResolver = channelResolver;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		Assert.isInstanceOf(ConfigurableBeanFactory.class, beanFactory,
				"a ConfigurableBeanFactory is required");
		this.beanFactory = (ConfigurableBeanFactory) beanFactory;
	}

	@Override
	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	@Override
	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	@Override
	public void afterPropertiesSet() {
		if (this.channelResolver == null) {
			this.channelResolver = new BeanFactoryMessageChannelDestinationResolver(this.beanFactory);
		}
		initializeAdapter();
	}

	@Override
	public SourcePollingChannelAdapter getObject() {
		if (this.adapter == null) {
			initializeAdapter();
		}
		return this.adapter;
	}

	@Override
	public Class<?> getObjectType() {
		return SourcePollingChannelAdapter.class;
	}

	private void initializeAdapter() {
		this.initializationMonitor.lock();
		try {
			if (this.initialized) {
				return;
			}
			Assert.notNull(this.source, "source is required");

			SourcePollingChannelAdapter spca = new SourcePollingChannelAdapter();
			spca.setSource(this.source);

			if (StringUtils.hasText(this.outputChannelName)) {
				Assert.isNull(this.outputChannel, "'outputChannelName' and 'outputChannel' are mutually exclusive.");
				spca.setOutputChannelName(this.outputChannelName);
			}
			else {
				Assert.notNull(this.outputChannel, "outputChannel is required");
				spca.setOutputChannel(this.outputChannel);
			}

			if (this.pollerMetadata == null) {
				this.pollerMetadata = PollerMetadata.getDefaultPollerMetadata(this.beanFactory);
				Assert.notNull(this.pollerMetadata, () -> "No poller has been defined for channel-adapter '"
						+ this.beanName + "', and no default poller is available within the context.");
			}
			long maxMessagesPerPoll = this.pollerMetadata.getMaxMessagesPerPoll();
			if (maxMessagesPerPoll == PollerMetadata.MAX_MESSAGES_UNBOUNDED) {
				// the default is 1 since a source might return
				// a non-null and non-interruptible value every time it is invoked
				maxMessagesPerPoll = 1;
			}
			spca.setMaxMessagesPerPoll(maxMessagesPerPoll);
			if (this.sendTimeout != null) {
				spca.setSendTimeout(this.sendTimeout);
			}
			spca.setTaskExecutor(this.pollerMetadata.getTaskExecutor());
			spca.setAdviceChain(this.pollerMetadata.getAdviceChain());
			spca.setTrigger(this.pollerMetadata.getTrigger());
			spca.setErrorHandler(this.pollerMetadata.getErrorHandler());
			spca.setBeanClassLoader(this.beanClassLoader);
			if (this.autoStartup != null) {
				spca.setAutoStartup(this.autoStartup);
			}
			spca.setPhase(this.phase);
			spca.setRole(this.role);
			spca.setBeanName(this.beanName);
			spca.setBeanFactory(this.beanFactory);
			spca.setTransactionSynchronizationFactory(this.pollerMetadata.getTransactionSynchronizationFactory());
			if (this.taskScheduler != null) {
				spca.setTaskScheduler(this.taskScheduler);
			}
			spca.afterPropertiesSet();
			this.adapter = spca;
			this.initialized = true;
		}
		finally {
			this.initializationMonitor.unlock();
		}
	}

	/*
	 * SmartLifecycle implementation (delegates to the created adapter)
	 */

	@Override
	public boolean isAutoStartup() {
		return (this.adapter == null) || this.adapter.isAutoStartup();
	}

	@Override
	public int getPhase() {
		return (this.adapter != null) ? this.adapter.getPhase() : 0;
	}

	@Override
	public boolean isRunning() {
		return (this.adapter != null) && this.adapter.isRunning();
	}

	@Override
	public void start() {
		if (this.adapter != null) {
			this.adapter.start();
		}
	}

	@Override
	public void stop() {
		if (this.adapter != null) {
			this.adapter.stop();
		}
	}

	@Override
	public void stop(Runnable callback) {
		if (this.adapter != null) {
			this.adapter.stop(callback);
		}
		else {
			callback.run();
		}
	}

	@Override
	public void destroy() {
		if (this.adapter != null) {
			this.adapter.destroy();
		}
	}

}
