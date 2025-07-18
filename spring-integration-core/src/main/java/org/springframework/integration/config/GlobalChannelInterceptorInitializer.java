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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.integration.channel.interceptor.GlobalChannelInterceptorWrapper;
import org.springframework.util.CollectionUtils;

/**
 * The {@link IntegrationConfigurationInitializer} to populate {@link GlobalChannelInterceptorWrapper}
 * for {@link org.springframework.messaging.support.ChannelInterceptor}s marked with
 * {@link GlobalChannelInterceptor} annotation.
 * <p>
 * {@link org.springframework.context.annotation.Bean} methods are also processed.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 4.0
 */
public class GlobalChannelInterceptorInitializer implements IntegrationConfigurationInitializer {

	@Override
	public void initialize(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;

		for (String beanName : registry.getBeanDefinitionNames()) {
			BeanDefinition beanDefinition = registry.getBeanDefinition(beanName);
			Map<String, @Nullable Object> interceptorAttributes = obtainGlobalChannelInterceptorAttributes(beanDefinition);
			if (!CollectionUtils.isEmpty(interceptorAttributes)) {
				BeanDefinitionBuilder builder =
						BeanDefinitionBuilder.genericBeanDefinition(GlobalChannelInterceptorWrapper.class)
								.addConstructorArgReference(beanName)
								.addPropertyValue("patterns", interceptorAttributes.get("patterns"))
								.addPropertyValue("order", interceptorAttributes.get("order"));

				BeanDefinitionReaderUtils.registerWithGeneratedName(builder.getBeanDefinition(), registry);
			}
		}
	}

	@Nullable
	private static Map<String, @Nullable Object> obtainGlobalChannelInterceptorAttributes(BeanDefinition beanDefinition) {
		Map<String, @Nullable Object> annotationAttributes = null;
		if (beanDefinition instanceof AnnotatedBeanDefinition annotatedBeanDefinition) {
			AnnotationMetadata metadata = annotatedBeanDefinition.getMetadata();
			annotationAttributes = metadata.getAnnotationAttributes(GlobalChannelInterceptor.class.getName());
			if (CollectionUtils.isEmpty(annotationAttributes)
					&& beanDefinition.getSource() instanceof MethodMetadata beanMethod) {

				annotationAttributes = beanMethod.getAnnotationAttributes(GlobalChannelInterceptor.class.getName());
			}
		}
		return annotationAttributes;
	}

}
