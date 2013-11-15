/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.support.channel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.integration.MessageChannel;
import org.springframework.integration.channel.registry.HeaderChannelRegistry;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.util.Assert;

/**
 * {@link ChannelResolver} implementation based on a Spring {@link BeanFactory}.
 *
 * <p>Will lookup Spring managed beans identified by bean name,
 * expecting them to be of type {@link MessageChannel}.
 *
 * @author Mark Fisher
 * @see org.springframework.beans.factory.BeanFactory
 */
public class BeanFactoryChannelResolver implements ChannelResolver, BeanFactoryAware {

	private final static Log logger = LogFactory.getLog(BeanFactoryChannelResolver.class);

	private volatile BeanFactory beanFactory;

	private volatile HeaderChannelRegistry replyChannelRegistry;

	/**
	 * Create a new instance of the {@link BeanFactoryChannelResolver} class.
	 * <p>The BeanFactory to access must be set via <code>setBeanFactory</code>.
	 * This will happen automatically if this resolver is defined within an
	 * ApplicationContext thereby receiving the callback upon initialization.
	 * @see #setBeanFactory
	 */
	public BeanFactoryChannelResolver() {
	}

	/**
	 * Create a new instance of the {@link BeanFactoryChannelResolver} class.
	 * <p>Use of this constructor is redundant if this object is being created
	 * by a Spring IoC container as the supplied {@link BeanFactory} will be
	 * replaced by the {@link BeanFactory} that creates it (c.f. the
	 * {@link BeanFactoryAware} contract). So only use this constructor if you
	 * are instantiating this object explicitly rather than defining a bean.
	 *
	 * @param beanFactory the bean factory to be used to lookup {@link MessageChannel}s.
	 */
	public BeanFactoryChannelResolver(BeanFactory beanFactory) {
		Assert.notNull(beanFactory, "BeanFactory must not be null");
		this.lookupHeaderChannelRegistry(beanFactory);
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
		this.lookupHeaderChannelRegistry(beanFactory);
	}

	private void lookupHeaderChannelRegistry(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
		try {
			this.replyChannelRegistry = beanFactory.getBean(
					IntegrationContextUtils.INTEGRATION_HEADER_CHANNEL_REGISTRY_BEAN_NAME,
					HeaderChannelRegistry.class);
		}
		catch (Exception e) {
			logger.warn("No HeaderChannelRegistry found", e);
		}
	}

	@Override
	public MessageChannel resolveChannelName(String name) {
		Assert.state(this.beanFactory != null, "BeanFactory is required");
		try {
			return this.beanFactory.getBean(name, MessageChannel.class);
		}
		catch (BeansException e) {
			if (this.replyChannelRegistry != null) {
				MessageChannel channel = this.replyChannelRegistry.channelNameToChannel(name);
				if (channel != null) {
					return channel;
				}
			}
			throw new ChannelResolutionException(
					"failed to look up MessageChannel bean with name '" + name + "'", e);
		}
	}

}
