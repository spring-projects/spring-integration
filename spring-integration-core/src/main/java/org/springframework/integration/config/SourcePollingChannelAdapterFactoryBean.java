/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.integration.config;

import org.springframework.beans.factory.BeanClassLoaderAware;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.core.MessageSource;
import org.springframework.integration.endpoint.SourcePollingChannelAdapter;
import org.springframework.integration.scheduling.PollerMetadata;
import org.springframework.util.Assert;

/**
 * FactoryBean for creating a SourcePollingChannelAdapter instance.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 */
public class SourcePollingChannelAdapterFactoryBean implements FactoryBean<SourcePollingChannelAdapter>,
		BeanFactoryAware, BeanNameAware, BeanClassLoaderAware, InitializingBean, SmartLifecycle {

	private volatile MessageSource<?> source;

	private volatile MessageChannel outputChannel;

	private volatile PollerMetadata pollerMetadata;

	private volatile boolean autoStartup = true;

	private volatile Long sendTimeout;

	private volatile String beanName;

	private volatile ConfigurableBeanFactory beanFactory;

	private volatile ClassLoader beanClassLoader;

	private volatile SourcePollingChannelAdapter adapter;

	private volatile boolean initialized;

	private final Object initializationMonitor = new Object();

	public void setSource(MessageSource<?> source) {
		this.source = source;
	}

	public void setSendTimeout(long sendTimeout) {
		this.sendTimeout = sendTimeout;
	}

	public void setOutputChannel(MessageChannel outputChannel) {
		this.outputChannel = outputChannel;
	}

	public void setPollerMetadata(PollerMetadata pollerMetadata) {
		this.pollerMetadata = pollerMetadata;
	}

	public void setAutoStartup(boolean autoStartup) {
		this.autoStartup = autoStartup;
	}

	public void setBeanFactory(BeanFactory beanFactory) {
		Assert.isInstanceOf(ConfigurableBeanFactory.class, beanFactory,
				"a ConfigurableBeanFactory is required");
		this.beanFactory = (ConfigurableBeanFactory) beanFactory;
	}

	public void setBeanClassLoader(ClassLoader classLoader) {
		this.beanClassLoader = classLoader;
	}

	public void setBeanName(String beanName) {
		this.beanName = beanName;
	}

	public void afterPropertiesSet() throws Exception {
		this.initializeAdapter();
	}

	public SourcePollingChannelAdapter getObject() throws Exception {
		if (this.adapter == null) {
			this.initializeAdapter();
		}
		return this.adapter;
	}

	public Class<?> getObjectType() {
		return SourcePollingChannelAdapter.class;
	}

	public boolean isSingleton() {
		return true;
	}

	private void initializeAdapter() {
		synchronized (this.initializationMonitor) {
			if (this.initialized) {
				return;
			}
			Assert.notNull(this.source, "source is required");
			Assert.notNull(this.outputChannel, "outputChannel is required");
			SourcePollingChannelAdapter spca = new SourcePollingChannelAdapter();
			spca.setSource(this.source);
			spca.setOutputChannel(this.outputChannel);
			if (this.pollerMetadata == null) {
				this.pollerMetadata = IntegrationContextUtils.getDefaultPollerMetadata(this.beanFactory);
				Assert.notNull(this.pollerMetadata, "No poller has been defined for channel-adapter '"
						+ this.beanName + "', and no default poller is available within the context.");
			}
			if (this.pollerMetadata.getMaxMessagesPerPoll() == Integer.MIN_VALUE){
				// the default is 1 since a source might return
				// a non-null and non-interruptable value every time it is invoked
				this.pollerMetadata.setMaxMessagesPerPoll(1);
			}
			spca.setMaxMessagesPerPoll(this.pollerMetadata.getMaxMessagesPerPoll());
			if (this.sendTimeout != null){
				spca.setSendTimeout(this.sendTimeout);
			}
			spca.setTaskExecutor(this.pollerMetadata.getTaskExecutor());
			spca.setAdviceChain(this.pollerMetadata.getAdviceChain());
			spca.setTrigger(this.pollerMetadata.getTrigger());
			spca.setErrorHandler(this.pollerMetadata.getErrorHandler());
			spca.setSynchronized(this.pollerMetadata.isSynchronized());
			spca.setBeanClassLoader(this.beanClassLoader);
			spca.setAutoStartup(this.autoStartup);
			spca.setBeanName(this.beanName);
			spca.setBeanFactory(this.beanFactory);
			spca.afterPropertiesSet();
			this.adapter = spca;
			this.initialized = true;
		}
	}

	/*
	 * SmartLifecycle implementation (delegates to the created adapter)
	 */

	public boolean isAutoStartup() {
		return (this.adapter != null) ? this.adapter.isAutoStartup() : true;
	}

	public int getPhase() {
		return (this.adapter != null) ? this.adapter.getPhase() : 0;
	}

	public boolean isRunning() {
		return (this.adapter != null) ? this.adapter.isRunning() : false;
	}

	public void start() {
		if (this.adapter != null) {
			this.adapter.start();
		}
	}

	public void stop() {
		if (this.adapter != null) {
			this.adapter.stop();
		}
	}

	public void stop(Runnable callback) {
		if (this.adapter != null) {
			this.adapter.stop(callback);
		}
	}
}
