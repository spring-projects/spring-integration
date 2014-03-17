/*
 * Copyright 2014 the original author or authors.
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

package org.springframework.integration.config;

import java.util.Map;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.integration.aop.PublisherAnnotationBeanPostProcessor;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.util.StringUtils;

/**
 * @author Artem Bilan
 * @since 4.0
 */
public class PublisherRegistrar implements ImportBeanDefinitionRegistrar {

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		Map<String, Object> annotationAttributes = importingClassMetadata.getAnnotationAttributes(EnablePublisher.class.getName());
		if (annotationAttributes == null) {
			return;
		}
		String value = (String) annotationAttributes.get("value");
		if (!registry.containsBeanDefinition(IntegrationContextUtils.PUBLISHER_ANNOTATION_POSTPROCESSOR_NAME)) {
			BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(PublisherAnnotationBeanPostProcessor.class)
					.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);

			if (StringUtils.hasText(value)) {
				builder.addPropertyReference("defaultChannel", value);
			}

			registry.registerBeanDefinition(IntegrationContextUtils.PUBLISHER_ANNOTATION_POSTPROCESSOR_NAME, builder.getBeanDefinition());
		}
		else {
			BeanDefinition beanDefinition = registry.getBeanDefinition(IntegrationContextUtils.PUBLISHER_ANNOTATION_POSTPROCESSOR_NAME);
			MutablePropertyValues propertyValues = beanDefinition.getPropertyValues();
			RuntimeBeanReference defaultChannel = (RuntimeBeanReference) propertyValues.getPropertyValue("defaultChannel").getValue();
			if (StringUtils.hasText(value)) {
				if (defaultChannel == null) {
					propertyValues.addPropertyValue("defaultChannel", new RuntimeBeanReference(value));
				}
				else if (!value.equals(defaultChannel.getBeanName())) {
					throw new BeanDefinitionStoreException("When more than one enable publisher definition " +
							"(@EnablePublisher or <annotation-config>)" +
							" is found in the context, they all must have the same 'default-publisher-channel' value.");
				}
			}
		}
	}

}
