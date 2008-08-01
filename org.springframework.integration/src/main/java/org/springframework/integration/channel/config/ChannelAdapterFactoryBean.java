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

package org.springframework.integration.channel.config;

import java.util.List;

import org.springframework.beans.factory.BeanNameAware;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.integration.ConfigurationException;
import org.springframework.integration.channel.AbstractMessageChannel;
import org.springframework.integration.channel.ChannelInterceptor;
import org.springframework.integration.channel.MessageChannel;
import org.springframework.integration.channel.PollableChannelAdapter;
import org.springframework.integration.channel.SubscribableChannelAdapter;
import org.springframework.integration.message.MessageSource;
import org.springframework.integration.message.MessageTarget;
import org.springframework.integration.message.PollableSource;
import org.springframework.integration.message.SubscribableSource;

/**
 * @author Mark Fisher
 */
public class ChannelAdapterFactoryBean implements FactoryBean, BeanNameAware {

	private volatile String name;

	private volatile MessageSource<?> source;

	private volatile MessageTarget target;

	private volatile AbstractMessageChannel channel;

	private List<ChannelInterceptor> interceptors;

	private volatile boolean initialized;

	private final Object initializationMonitor = new Object();


	public void setBeanName(String name) {
		this.name = name;
	}

	public void setSource(MessageSource<?> source) {
		this.source = source;
	}

	public void setTarget(MessageTarget target) {
		this.target = target;
	}

	public void setInterceptors(List<ChannelInterceptor> interceptors) {
		this.interceptors = interceptors;
	}

	public Object getObject() throws Exception {
		if (!this.initialized) {
			this.initializeChannel();
		}
		return this.channel;
	}

	public Class<?> getObjectType() {
		if (!this.initialized) {
			return MessageChannel.class;
		}
		return this.channel.getClass();
	}

	public boolean isSingleton() {
		return true;
	}

	private void initializeChannel() {
		synchronized (this.initializationMonitor) {
			if (this.initialized) {
				return;
			}
			if (this.source == null || source instanceof PollableSource) {
				this.channel = new PollableChannelAdapter(
						this.name, (PollableSource<?>) this.source, this.target);
			}
			else if (this.source instanceof SubscribableSource) {
				this.channel = new SubscribableChannelAdapter(
						this.name, (SubscribableSource) this.source, this.target);
			}
			else {
				throw new ConfigurationException("source must be either a PollableSource or SubscribableSource");
			}
			if (this.interceptors != null) {
				this.channel.setInterceptors(this.interceptors);
			}
		}
	}

}
