/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.config;

import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.ManagedSet;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.integration.history.MessageHistoryConfigurer;

/**
 * Registers the {@link MessageHistoryConfigurer} {@link org.springframework.beans.factory.config.BeanDefinition}
 * for {@link org.springframework.integration.history.MessageHistory}.
 * This registrar is applied from {@code @EnableMessageHistory} on the {@code Configuration} class
 * or from {@code MessageHistoryParser}.
 *
 * @author Artem Bilan
 * @since 4.0
 */
public class MessageHistoryRegistrar implements ImportBeanDefinitionRegistrar {

	private static final String MESSAGE_HISTORY_CONFIGURER = MessageHistoryConfigurer.class.getName();

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		Map<String,Object> annotationAttributes = importingClassMetadata.getAnnotationAttributes(EnableMessageHistory.class.getName());
		Object componentNamePatterns = annotationAttributes.get("value");

		if (componentNamePatterns instanceof String[]) {
			StringBuilder componentNamePatternsString = new StringBuilder();
			for (String s : (String[]) componentNamePatterns) {
				componentNamePatternsString.append(s).append(",");
			}
			componentNamePatterns = componentNamePatternsString.substring(0, componentNamePatternsString.length() - 1);
		}

		if (!registry.containsBeanDefinition(MESSAGE_HISTORY_CONFIGURER)) {
			Set<Object> componentNamePatternsSet = new ManagedSet<Object>();
			componentNamePatternsSet.add(componentNamePatterns);

			AbstractBeanDefinition messageHistoryConfigurer = BeanDefinitionBuilder.genericBeanDefinition(MessageHistoryConfigurer.class)
					.addPropertyValue("componentNamePatternsSet", componentNamePatternsSet)
					.getBeanDefinition();

			registry.registerBeanDefinition(MESSAGE_HISTORY_CONFIGURER, messageHistoryConfigurer);

		}
		else {
			@SuppressWarnings("unchecked")
			Set<Object> currentComponentNamePatternsSet = (Set<Object>) registry.getBeanDefinition(MESSAGE_HISTORY_CONFIGURER)
					.getPropertyValues().getPropertyValue("componentNamePatternsSet").getValue();
			currentComponentNamePatternsSet.add(componentNamePatterns);
		}
	}

}
