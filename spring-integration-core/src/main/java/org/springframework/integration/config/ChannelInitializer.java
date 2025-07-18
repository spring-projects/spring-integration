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

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.util.Assert;

/**
 * An {@link InitializingBean} implementation that is responsible for creating
 * channels that are not explicitly defined but identified via the 'input-channel'
 * attribute of the corresponding endpoints.
 * <p>
 * This bean plays a role of pre-instantiator since it is instantiated and
 * initialized as the very first bean of all Spring Integration beans using
 * {@link org.springframework.integration.config.xml.AbstractIntegrationNamespaceHandler}.
 *
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.1.1
 */
public final class ChannelInitializer implements BeanFactoryAware, InitializingBean {

	private static final Log LOGGER = LogFactory.getLog(ChannelInitializer.class);

	@SuppressWarnings("NullAway.Init")
	private volatile DefaultListableBeanFactory beanFactory;

	private volatile boolean autoCreate = true;

	ChannelInitializer() {
	}

	public void setAutoCreate(boolean autoCreate) {
		this.autoCreate = autoCreate;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = (DefaultListableBeanFactory) beanFactory;
	}

	@Override
	public void afterPropertiesSet() {
		Assert.notNull(this.beanFactory, "'beanFactory' must not be null");
		if (this.autoCreate) {
			AutoCreateCandidatesCollector channelCandidatesCollector =
					this.beanFactory.getBean(IntegrationContextUtils.AUTO_CREATE_CHANNEL_CANDIDATES_BEAN_NAME,
							AutoCreateCandidatesCollector.class);
			// at this point channelNames are all resolved with placeholders and SpEL
			Collection<String> channelNames = channelCandidatesCollector.channelNames;
			if (channelNames != null) {
				for (String channelName : channelNames) {
					if (!this.beanFactory.containsBean(channelName)) {
						if (LOGGER.isDebugEnabled()) {
							LOGGER.debug("Auto-creating channel '" + channelName + "' as DirectChannel");
						}
						DirectChannel channelToRegister = new DirectChannel();
						this.beanFactory.registerSingleton(channelName, channelToRegister);
						this.beanFactory.initializeBean(channelToRegister, channelName);
					}
				}
			}
		}
	}

	/**
	 * Collects candidate channel names to be auto-created by {@link ChannelInitializer}.
	 * @param channelNames the auto-create candidate channel bean names.
	 */
	public record AutoCreateCandidatesCollector(Collection<String> channelNames) {

	}

}
