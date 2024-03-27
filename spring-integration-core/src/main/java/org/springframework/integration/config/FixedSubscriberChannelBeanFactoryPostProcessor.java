/*
 * Copyright 2014-2024 the original author or authors.
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

import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.config.ConstructorArgumentValues;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.integration.channel.FixedSubscriberChannel;
import org.springframework.util.Assert;

/**
 * Used to post process candidates for {@link FixedSubscriberChannel}
 * {@link org.springframework.messaging.MessageHandler}s.
 * @author Gary Russell
 * @since 4.0
 *
 */
public final class FixedSubscriberChannelBeanFactoryPostProcessor implements BeanFactoryPostProcessor {

	private final Map<String, String> candidateFixedChannelHandlerMap;

	FixedSubscriberChannelBeanFactoryPostProcessor(Map<String, String> candidateHandlers) {
		this.candidateFixedChannelHandlerMap = candidateHandlers;
	}

	@Override
	public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		if (beanFactory instanceof BeanDefinitionRegistry) {
			BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
			if (this.candidateFixedChannelHandlerMap.size() > 0) {
				for (Entry<String, String> entry : this.candidateFixedChannelHandlerMap.entrySet()) {
					String handlerName = entry.getKey();
					String channelName = entry.getValue();
					BeanDefinition handlerBeanDefinition = null;
					if (registry.containsBeanDefinition(handlerName)) {
						handlerBeanDefinition = registry.getBeanDefinition(handlerName);
					}
					if (handlerBeanDefinition != null && registry.containsBeanDefinition(channelName)) {
						BeanDefinition inputChannelDefinition = registry.getBeanDefinition(channelName);
						if (FixedSubscriberChannel.class.getName().equals(inputChannelDefinition.getBeanClassName())) {
							ConstructorArgumentValues constructorArgumentValues = inputChannelDefinition
									.getConstructorArgumentValues();
							Assert.isTrue(constructorArgumentValues.isEmpty(),
									"Only one subscriber is allowed for a FixedSubscriberChannel.");
							constructorArgumentValues.addGenericArgumentValue(handlerBeanDefinition);
						}
					}
				}
			}
		}
	}

}
