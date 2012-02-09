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

import java.util.Set;

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
import org.springframework.integration.context.IntegrationContextUtils;
/**
 * A {@link InitializingBean} implementation that is responsible for creating channels that
 * are not explicitly defined but identified via 'input-channel' attribute of the corresponding endpoints.
 *
 * This bean plays a role of pre-instantiator since it is instantiated and initialized as the
 * very first bean of all SI beans using {@link AbstractIntegrationNamespaceHandler}.
 *
 * @author Oleg Zhurakousky
 * @since 2.1
 */
class ChannelInitializer implements BeanFactoryAware, InitializingBean {

	private Log logger = LogFactory.getLog(this.getClass());

	private volatile BeanFactory beanFactory;

	private final Set<String> channelNames;
	
	public ChannelInitializer(Set<String> channelNames){
		this.channelNames = channelNames;
	}

	private boolean shouldAutoCreateChannel(String channelName) {
		return !IntegrationContextUtils.ERROR_CHANNEL_BEAN_NAME.equals(channelName)
				&& !IntegrationContextUtils.NULL_CHANNEL_BEAN_NAME.equals(channelName);
	}

	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	public void afterPropertiesSet() throws Exception {
		for (String channelName : this.channelNames) {
			if (!beanFactory.containsBean(channelName) && this.shouldAutoCreateChannel(channelName)){
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