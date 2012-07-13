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
package org.springframework.integration.config.xml;

import java.util.Collection;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.util.Assert;

/**
 * A {@link InitializingBean} implementation that is responsible for creating
 * channels that are not explicitly defined but identified via the 'input-channel'
 * attribute of the corresponding endpoints.
 *
 * This bean plays a role of pre-instantiator since it is instantiated and
 * initialized as the very first bean of all SI beans using
 * {@link AbstractIntegrationNamespaceHandler}.
 *
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @since 2.1.1
 */
final class ChannelInitializer implements BeanFactoryAware, InitializingBean {

	public static String AUTO_CREATE_CHANNEL_CANDIDATES_BEAN_NAME = "$autoCreateChannelCandidates";

	public static String CHANNEL_NAMES_ATTR = "channelNames";

	private Log logger = LogFactory.getLog(this.getClass());

	private volatile BeanFactory beanFactory;

	private volatile boolean autoCreate = true;

	private volatile int defaultMaxUnicastSubscribers = Integer.MAX_VALUE;

	private volatile int defaultMaxBroadcastSubscribers = Integer.MAX_VALUE;

	public void setAutoCreate(boolean autoCreate) {
		this.autoCreate = autoCreate;
	}

	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	public int getDefaultMaxUnicastSubscribers() {
		return defaultMaxUnicastSubscribers;
	}

	/**
	 * Set the default max-subscribers for all unicasting channels that don't have the
	 * attribute set on their dispatcher. Default {@link Integer#MAX_VALUE}.
	 * @param defaultMaxUnicastSubscribers
	 */
	public void setDefaultMaxUnicastSubscribers(int defaultMaxUnicastSubscribers) {
		this.defaultMaxUnicastSubscribers = defaultMaxUnicastSubscribers;
	}

	public int getDefaultMaxBroadcastSubscribers() {
		return defaultMaxBroadcastSubscribers;
	}

	/**
	 * Set the default max-subscribers for all broadcasting (pub-sub) channels that don't have the
	 * attribute set. Default {@link Integer#MAX_VALUE}.
	 * @param defaultMaxBroadcastSubscribers
	 */
	public void setDefaultMaxBroadcastSubscribers(int defaultMaxBroadcastSubscribers) {
		this.defaultMaxBroadcastSubscribers = defaultMaxBroadcastSubscribers;
	}

	public void afterPropertiesSet() throws Exception {
		Assert.notNull(this.beanFactory, "'beanFactory' must not be null");
		if (!autoCreate){
			return;
		}
		else {
			AutoCreateCandidatesCollector channelCandidatesCollector  =
					beanFactory.getBean(AUTO_CREATE_CHANNEL_CANDIDATES_BEAN_NAME, AutoCreateCandidatesCollector.class);
			Assert.notNull(channelCandidatesCollector, "Failed to locate '" +
					ChannelInitializer.AUTO_CREATE_CHANNEL_CANDIDATES_BEAN_NAME);
			// at this point channelNames are all resolved with placeholders and SpEL
			Collection<String> channelNames = channelCandidatesCollector.getChannelNames();
			if (channelNames != null){
				for (String channelName : channelNames) {
					if (!beanFactory.containsBean(channelName)){
						if (this.logger.isDebugEnabled()){
							this.logger.debug("Auto-creating channel '" + channelName + "' as DirectChannel");
						}
						RootBeanDefinition messageChannel = new RootBeanDefinition();
						messageChannel.setBeanClass(DirectChannel.class);
						BeanDefinitionHolder messageChannelHolder = new BeanDefinitionHolder(messageChannel, channelName);
						BeanDefinitionReaderUtils.registerBeanDefinition(messageChannelHolder, (BeanDefinitionRegistry) this.beanFactory);
					}
				}
			}
		}
	}

	/*
	 * Collects candidate channel names to be auto-created by ChannelInitializer
	 */
	static class AutoCreateCandidatesCollector {

		private final Collection<String> channelNames;

		public AutoCreateCandidatesCollector(Collection<String> channelNames){
			this.channelNames = channelNames;
		}

		public Collection<String> getChannelNames() {
			return channelNames;
		}
	}
}