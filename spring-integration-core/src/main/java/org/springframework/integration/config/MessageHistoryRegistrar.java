/*
 * Copyright 2014-present the original author or authors.
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
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.history.MessageHistoryConfigurer;

/**
 * Registers the {@link MessageHistoryConfigurer} {@link org.springframework.beans.factory.config.BeanDefinition}
 * for {@link org.springframework.integration.history.MessageHistory}.
 * This registrar is applied from {@code @EnableMessageHistory} on the {@code Configuration} class
 * or from {@code MessageHistoryParser}.
 *
 * @author Artem Bilan
 * @author Gary Russell
 * @author Chris Bono
 *
 * @since 4.0
 */
public class MessageHistoryRegistrar implements ImportBeanDefinitionRegistrar {

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		if (registry.containsBeanDefinition(IntegrationContextUtils.INTEGRATION_MESSAGE_HISTORY_CONFIGURER_BEAN_NAME)) {
			throw new BeanDefinitionStoreException(
					"Only one @EnableMessageHistory or <message-history/> can be declared in the application context.");
		}

		Map<String, @Nullable Object> annotationAttributes =
				importingClassMetadata.getAnnotationAttributes(EnableMessageHistory.class.getName());
		Object componentNamePatterns = Objects.requireNonNull(annotationAttributes).get("value"); // NOSONAR never null

		String patterns;

		if (componentNamePatterns instanceof String[]) {
			patterns = String.join(",", (String[]) componentNamePatterns);
		}
		else {
			patterns = (String) componentNamePatterns;
		}

		BeanDefinition messageHistoryConfigurer =
				BeanDefinitionBuilder.genericBeanDefinition(MessageHistoryConfigurer.class)
						.addPropertyValue("componentNamePatterns", patterns)
						.getBeanDefinition();

		registry.registerBeanDefinition(IntegrationContextUtils.INTEGRATION_MESSAGE_HISTORY_CONFIGURER_BEAN_NAME,
				messageHistoryConfigurer);
	}

}
