/*
 * Copyright 2014-2021 the original author or authors.
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

import java.util.Arrays;
import java.util.Map;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.history.MessageHistoryConfigurer;
import org.springframework.util.StringUtils;

/**
 * Registers the {@link MessageHistoryConfigurer} {@link org.springframework.beans.factory.config.BeanDefinition}
 * for {@link org.springframework.integration.history.MessageHistory}.
 * This registrar is applied from {@code @EnableMessageHistory} on the {@code Configuration} class
 * or from {@code MessageHistoryParser}.
 *
 * @author Artem Bilan
 * @author Gary Russell
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

		Map<String, Object> annotationAttributes =
				importingClassMetadata.getAnnotationAttributes(EnableMessageHistory.class.getName());
		Object componentNamePatterns = annotationAttributes.get("value"); // NOSONAR never null

		String patterns;

		if (componentNamePatterns instanceof String[]) {
			patterns = String.join(",", (String[]) componentNamePatterns);
		}
		else {
			patterns = (String) componentNamePatterns;
		}

		registry.registerBeanDefinition(IntegrationContextUtils.INTEGRATION_MESSAGE_HISTORY_CONFIGURER_BEAN_NAME,
				new RootBeanDefinition(MessageHistoryConfigurer.class,
						() -> createMessageHistoryConfigurer(registry, patterns)));
	}

	private MessageHistoryConfigurer createMessageHistoryConfigurer(BeanDefinitionRegistry registry, String patterns) {
		MessageHistoryConfigurer messageHistoryConfigurer = new MessageHistoryConfigurer();
		if (StringUtils.hasText(patterns)) {
			ConfigurableBeanFactory beanFactory = null;
			if (registry instanceof ConfigurableBeanFactory) {
				beanFactory = (ConfigurableBeanFactory) registry;
			}
			else if (registry instanceof ConfigurableApplicationContext) {
				beanFactory = ((ConfigurableApplicationContext) registry).getBeanFactory();
			}

			String[] patternsToSet = StringUtils.delimitedListToStringArray(patterns, ",", " ");
			if (beanFactory != null) {
				patternsToSet =
						Arrays.stream(patternsToSet)
								.map(beanFactory::resolveEmbeddedValue)
								.flatMap((pattern) ->
										Arrays.stream(StringUtils.delimitedListToStringArray(pattern, ",", " ")))
								.toArray(String[]::new);
			}
			messageHistoryConfigurer.setComponentNamePatterns(patternsToSet);
		}

		return messageHistoryConfigurer;
	}

}
