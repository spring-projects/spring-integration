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

import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.integration.aop.PublisherAnnotationBeanPostProcessor;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.StringUtils;

/**
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 4.0
 */
public class PublisherRegistrar implements ImportBeanDefinitionRegistrar {

	private static final Log LOGGER = LogFactory.getLog(PublisherRegistrar.class);

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		if (registry.containsBeanDefinition(IntegrationContextUtils.PUBLISHER_ANNOTATION_POSTPROCESSOR_NAME)) {
			throw new BeanDefinitionStoreException("Only one enable publisher definition " +
					"(@EnablePublisher or <annotation-config>) can be declared in the application context.");
		}
		Map<String, Object> annotationAttributes =
				importingClassMetadata.getAnnotationAttributes(EnablePublisher.class.getName());

		ConfigurableBeanFactory beanFactory;
		if (registry instanceof ConfigurableBeanFactory) {
			beanFactory = (ConfigurableBeanFactory) registry;
		}
		else if (registry instanceof ConfigurableApplicationContext) {
			beanFactory = ((ConfigurableApplicationContext) registry).getBeanFactory();
		}
		else {
			beanFactory = null;
		}

		BeanDefinitionBuilder builder =
				BeanDefinitionBuilder.genericBeanDefinition(PublisherAnnotationBeanPostProcessor.class,
						() -> createPublisherAnnotationBeanPostProcessor(annotationAttributes, beanFactory))
						.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);

		registry.registerBeanDefinition(IntegrationContextUtils.PUBLISHER_ANNOTATION_POSTPROCESSOR_NAME,
				builder.getBeanDefinition());
	}

	private PublisherAnnotationBeanPostProcessor createPublisherAnnotationBeanPostProcessor(
			@Nullable Map<String, Object> annotationAttributes, @Nullable ConfigurableBeanFactory beanFactory) {

		PublisherAnnotationBeanPostProcessor postProcessor = new PublisherAnnotationBeanPostProcessor();
		String defaultChannel =
				annotationAttributes == null
						? (String) AnnotationUtils.getDefaultValue(EnablePublisher.class)
						: (String) annotationAttributes.get("defaultChannel");
		if (StringUtils.hasText(defaultChannel)) {
			if (beanFactory != null) {
				defaultChannel = beanFactory.resolveEmbeddedValue(defaultChannel);
			}
			postProcessor.setDefaultChannelName(defaultChannel);
			if (LOGGER.isInfoEnabled()) {
				LOGGER.info("Setting '@Publisher' default-output-channel to '" + defaultChannel + "'.");
			}
		}
		if (annotationAttributes != null) {
			String proxyTargetClass = annotationAttributes.get("proxyTargetClass").toString();
			if (beanFactory != null) {
				proxyTargetClass = beanFactory.resolveEmbeddedValue(proxyTargetClass);
			}
			postProcessor.setProxyTargetClass(Boolean.parseBoolean(proxyTargetClass));

			String order = annotationAttributes.get("order").toString();
			if (beanFactory != null) {
				order = beanFactory.resolveEmbeddedValue(order);
			}
			if (StringUtils.hasText(order)) {
				postProcessor.setOrder(Integer.parseInt(order));
			}
		}
		return postProcessor;
	}

}
