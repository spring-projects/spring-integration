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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.annotation.ImportBeanDefinitionRegistrar;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.integration.aop.PublisherAnnotationBeanPostProcessor;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.util.StringUtils;

/**
 * @author Artem Bilan
 * @author Gary Russell
 * @since 4.0
 */
public class PublisherRegistrar implements ImportBeanDefinitionRegistrar {

	private static final Log logger = LogFactory.getLog(PublisherRegistrar.class);

	@Override
	public void registerBeanDefinitions(AnnotationMetadata importingClassMetadata, BeanDefinitionRegistry registry) {
		Map<String, Object> annotationAttributes =
				importingClassMetadata.getAnnotationAttributes(EnablePublisher.class.getName());
		if (annotationAttributes == null) {
			return;
		}
		String value = (String) annotationAttributes.get("value");
		if (!registry.containsBeanDefinition(IntegrationContextUtils.PUBLISHER_ANNOTATION_POSTPROCESSOR_NAME)) {
			BeanDefinitionBuilder builder =
					BeanDefinitionBuilder.genericBeanDefinition(PublisherAnnotationBeanPostProcessor.class)
							.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);

			if (StringUtils.hasText(value)) {
				builder.addPropertyValue("defaultChannelName", value);
				if (logger.isInfoEnabled()) {
					logger.info("Setting '@Publisher' default-output-channel to '" + value + "'.");
				}
			}

			registry.registerBeanDefinition(IntegrationContextUtils.PUBLISHER_ANNOTATION_POSTPROCESSOR_NAME,
					builder.getBeanDefinition());
		}
		else {
			BeanDefinition beanDefinition =
					registry.getBeanDefinition(IntegrationContextUtils.PUBLISHER_ANNOTATION_POSTPROCESSOR_NAME);
			MutablePropertyValues propertyValues = beanDefinition.getPropertyValues();
			PropertyValue defaultChannelPropertyValue = propertyValues.getPropertyValue("defaultChannelName");
			if (StringUtils.hasText(value)) {
				if (defaultChannelPropertyValue == null) {
					propertyValues.addPropertyValue("defaultChannelName", value);
					if (logger.isInfoEnabled()) {
						logger.info("Setting '@Publisher' default-output-channel to '" + value + "'.");
					}
				}
				else if (!value.equals(defaultChannelPropertyValue.getValue())) {
					throw new BeanDefinitionStoreException("When more than one enable publisher definition " +
							"(@EnablePublisher or <annotation-config>)" +
							" is found in the context, they all must have the same 'default-publisher-channel' value.");
				}
			}
		}
	}

}
