/*
 * Copyright 2002-2020 the original author or authors.
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

package org.springframework.integration.support.channel;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.core.DestinationResolutionException;
import org.springframework.messaging.core.DestinationResolver;
import org.springframework.util.Assert;

/**
 * {@link DestinationResolver} implementation based on a Spring {@link BeanFactory}.
 *
 * <p>Will lookup Spring managed beans identified by bean name,
 * expecting them to be of type {@link MessageChannel}.
 *
 * Consults a {@link HeaderChannelRegistry}, if available, if the bean is not found.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @see BeanFactory
 */
public class BeanFactoryChannelResolver implements DestinationResolver<MessageChannel>, BeanFactoryAware {

	private static final Log LOGGER = LogFactory.getLog(BeanFactoryChannelResolver.class);

	private BeanFactory beanFactory;

	private HeaderChannelRegistry replyChannelRegistry;

	private volatile boolean initialized;

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
	 * @param beanFactory the bean factory to be used to lookup {@link MessageChannel}s.
	 */
	public BeanFactoryChannelResolver(BeanFactory beanFactory) {
		Assert.notNull(beanFactory, "BeanFactory must not be null");
		this.beanFactory = beanFactory;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	@Override
	public MessageChannel resolveDestination(String name) {
		Assert.state(this.beanFactory != null, "BeanFactory is required");
		try {
			return this.beanFactory.getBean(name, MessageChannel.class);
		}
		catch (BeansException e) {
			if (!(e instanceof NoSuchBeanDefinitionException)) { // NOSONAR
				throw new DestinationResolutionException("A bean definition with name '"
						+ name + "' exists, but failed to be created", e);
			}
			if (!this.initialized) {
				synchronized (this) {
					if (!this.initialized) {
						try {
							this.replyChannelRegistry = this.beanFactory.getBean(
									IntegrationContextUtils.INTEGRATION_HEADER_CHANNEL_REGISTRY_BEAN_NAME,
									HeaderChannelRegistry.class);
						}
						catch (Exception ex) {
							LOGGER.debug("No HeaderChannelRegistry found");
						}
						this.initialized = true;
					}
				}
			}
			if (this.replyChannelRegistry != null) {
				MessageChannel channel = this.replyChannelRegistry.channelNameToChannel(name);
				if (channel != null) {
					return channel;
				}
			}
			throw new DestinationResolutionException("failed to look up MessageChannel with name '" + name
					+ "' in the BeanFactory"
					+ (this.replyChannelRegistry == null ? " (and there is no HeaderChannelRegistry present)." : "."),
					e);
		}
	}

}
