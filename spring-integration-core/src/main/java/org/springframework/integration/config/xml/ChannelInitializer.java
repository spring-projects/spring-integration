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
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.ManagedSet;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.integration.channel.DirectChannel;

/**
 * A {@link InitializingBean} implementation that is responsible for creating
 * channels that are not explicitly defined but identified via 'input-channel'
 * attribute of the corresponding endpoints.
 * 
 * This bean plays a role of pre-instantiator since it is instantiated and
 * initialized as the very first bean of all SI beans using
 * {@link AbstractIntegrationNamespaceHandler}.
 * 
 * @author Oleg Zhurakousky
 * @since 2.1.1
 */
final class ChannelInitializer implements BeanFactoryAware, InitializingBean {

	public static String AUTO_CREATE_CHANNEL_CANDIDATES_BEAN_NAME = "$autoCreateChannelCandidates";
	public static String CHANNEL_NAMES_ATTR = "channelNames";

	private Log logger = LogFactory.getLog(this.getClass());

	private volatile BeanFactory beanFactory;

	private volatile boolean autoCreate = true;
	
	public void setAutoCreate(boolean autoCreate) {
		this.autoCreate = autoCreate;  
	}

	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	public void afterPropertiesSet() throws Exception {
		if (!autoCreate){
			return;
		}
		else {
			AutoCreateCandidatesCollector channelCandidatesColector  = 
					(AutoCreateCandidatesCollector) beanFactory.getBean(AUTO_CREATE_CHANNEL_CANDIDATES_BEAN_NAME, AutoCreateCandidatesCollector.class);
			// at this point channelNames are all resolved with placeholders and SpEL
			Collection<String> channelNames = channelCandidatesColector.getChannelNames();
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
	 * This class serves two purposes
	 * 1. A BeanDefinition that collects candidate channel names as its BeanDefinition attributes
	 * 2. A holder of the resolved (property placeholders and SpEL expressions) channel names to be auto-created by the 
	 * ChannelInitializer.
	 * Once this bean reaches Factory post processing phase it creates a new definition of itself injecting it with
	 * the ManagedSet of channelNames property. The ManagedSet will be naturally resolved before it is used by the 
	 * ChannelInitializer. It also unregisters its old definition (since its only value was to provide a place
	 * to collect candidate channel names) and registers new definition of itself with "to-be resolved" channelNames
	 * property
	 */
	public static class AutoCreateCandidatesCollector implements BeanFactoryPostProcessor{
		
		private volatile Collection<String> channelNames;
		
		public void setChannelNames(Collection<String> channelNames) {
			this.channelNames = channelNames;
		}

		public Collection<String> getChannelNames() {
			return channelNames;
		}

		@SuppressWarnings("unchecked")
		public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
			BeanDefinition definition = beanFactory.getBeanDefinition(ChannelInitializer.AUTO_CREATE_CHANNEL_CANDIDATES_BEAN_NAME);
			Collection<String> _channelNames = (Collection<String>) definition.getAttribute(ChannelInitializer.CHANNEL_NAMES_ATTR);
			if (_channelNames != null){
				((BeanDefinitionRegistry)beanFactory).removeBeanDefinition(ChannelInitializer.AUTO_CREATE_CHANNEL_CANDIDATES_BEAN_NAME);
				BeanDefinitionBuilder candidatesBuilder = BeanDefinitionBuilder.genericBeanDefinition(this.getClass().getName());
				ManagedSet<String> candidateChannelNamesToBeResolved = new ManagedSet<String>();
				candidateChannelNamesToBeResolved.addAll(_channelNames);
				candidatesBuilder.addPropertyValue("channelNames", candidateChannelNamesToBeResolved);
				BeanDefinitionHolder holder = new BeanDefinitionHolder(candidatesBuilder.getBeanDefinition(), 
						ChannelInitializer.AUTO_CREATE_CHANNEL_CANDIDATES_BEAN_NAME);
				BeanDefinitionReaderUtils.registerBeanDefinition(holder, (BeanDefinitionRegistry) beanFactory);		
			}
		}
	}
}