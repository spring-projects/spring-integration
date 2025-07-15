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

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.integration.aop.PublisherAnnotationBeanPostProcessor;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.util.StringUtils;

/**
 * @author Artem Bilan
 * @author Gary Russell
 * @author Chris Bono
 *
 * @since 4.0
 */
public class PublisherRegistrar implements ImportBeanDefinitionRegistrar {

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		if (registry.containsBeanDefinition(IntegrationContextUtils.PUBLISHER_ANNOTATION_POSTPROCESSOR_NAME)) {
			throw new BeanDefinitionStoreException("Only one enable publisher definition " +
					"(@EnablePublisher or <annotation-config>) can be declared in the application context.");
		}
		Map<String, @Nullable Object> annotationAttributes =
				importingClassMetadata.getAnnotationAttributes(EnablePublisher.class.getName());

		String defaultChannel =
				annotationAttributes == null
						? (String) AnnotationUtils.getDefaultValue(EnablePublisher.class)
						: (String) annotationAttributes.get("defaultChannel");

		BeanDefinitionBuilder builder =
				BeanDefinitionBuilder.genericBeanDefinition(PublisherAnnotationBeanPostProcessor.class)
						.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);

		if (StringUtils.hasText(defaultChannel)) {
			builder.addPropertyValue("defaultChannelName", defaultChannel);
		}

		if (annotationAttributes != null) {
			builder.addPropertyValue("proxyTargetClass", annotationAttributes.get("proxyTargetClass"))
					.addPropertyValue("order", annotationAttributes.get("order"));
		}
		registry.registerBeanDefinition(IntegrationContextUtils.PUBLISHER_ANNOTATION_POSTPROCESSOR_NAME,
				builder.getBeanDefinition());
	}

}
