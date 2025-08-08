/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.util.Assert;

/**
 * A {@link InitializingBean} implementation that is responsible for creating
 * channels that are not explicitly defined but identified via the 'input-channel'
 * attribute of the corresponding endpoints.
 *
 * This bean plays a role of pre-instantiator since it is instantiated and
 * initialized as the very first bean of all SI beans using
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

	private volatile BeanFactory beanFactory;

	private volatile boolean autoCreate = true;

	ChannelInitializer() {
	}

	public void setAutoCreate(boolean autoCreate) {
		this.autoCreate = autoCreate;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@Override
	public void afterPropertiesSet() {
		Assert.notNull(this.beanFactory, "'beanFactory' must not be null");
		if (!this.autoCreate) {
			return;
		}
		else {
			AutoCreateCandidatesCollector channelCandidatesCollector =
					this.beanFactory.getBean(IntegrationContextUtils.AUTO_CREATE_CHANNEL_CANDIDATES_BEAN_NAME,
							AutoCreateCandidatesCollector.class);
			// at this point channelNames are all resolved with placeholders and SpEL
			Collection<String> channelNames = channelCandidatesCollector.getChannelNames();
			if (channelNames != null) {
				for (String channelName : channelNames) {
					if (!this.beanFactory.containsBean(channelName)) {
						if (LOGGER.isDebugEnabled()) {
							LOGGER.debug("Auto-creating channel '" + channelName + "' as DirectChannel");
						}
						IntegrationConfigUtils.autoCreateDirectChannel(channelName,
								(BeanDefinitionRegistry) this.beanFactory);
					}
				}
			}
		}
	}

	/*
	 * Collects candidate channel names to be auto-created by ChannelInitializer
	 */
	public static class AutoCreateCandidatesCollector {

		private final Collection<String> channelNames;

		AutoCreateCandidatesCollector(Collection<String> channelNames) {
			this.channelNames = channelNames;
		}

		public Collection<String> getChannelNames() {
			return this.channelNames;
		}

	}

}
