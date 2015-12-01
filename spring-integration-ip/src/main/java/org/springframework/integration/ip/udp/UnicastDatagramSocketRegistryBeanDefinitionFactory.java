/*
 * Copyright 2002-2015 the original author or authors.
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

package org.springframework.integration.ip.udp;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;

/**
 * TODO
 */
public class UnicastDatagramSocketRegistryBeanDefinitionFactory {

	public static BeanDefinition getBeanDefinition(BeanDefinitionRegistry beanDefinitionRegistry) {
		if (beanDefinitionRegistry.containsBeanDefinition(UnicastDatagramSocketRegistry.BEAN_NAME)) {
			return beanDefinitionRegistry.getBeanDefinition(UnicastDatagramSocketRegistry.BEAN_NAME);
		} else {
			BeanDefinition beanDefinition = BeanDefinitionBuilder.genericBeanDefinition(
					UnicastDatagramSocketRegistry.class).getBeanDefinition();
			beanDefinition.setScope(BeanDefinition.SCOPE_SINGLETON);
			beanDefinitionRegistry.registerBeanDefinition(UnicastDatagramSocketRegistry.BEAN_NAME,
					beanDefinition);
			return beanDefinition;
		}
	}
}
