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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanExpressionContext;
import org.springframework.beans.factory.config.BeanExpressionResolver;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.core.type.MethodMetadata;
import org.springframework.integration.channel.interceptor.GlobalChannelInterceptorWrapper;
import org.springframework.messaging.support.ChannelInterceptor;
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

	private ConfigurableListableBeanFactory beanFactory;

	private BeanExpressionContext beanExpressionContext;

	@Override
	public void initialize(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
		BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;
		this.beanExpressionContext = new BeanExpressionContext(beanFactory, null);
		for (String beanName : registry.getBeanDefinitionNames()) {
			BeanDefinition beanDefinition = registry.getBeanDefinition(beanName);
			if (beanDefinition instanceof AnnotatedBeanDefinition) {
				AnnotationMetadata metadata = ((AnnotatedBeanDefinition) beanDefinition).getMetadata();
				Map<String, Object> annotationAttributes =
						metadata.getAnnotationAttributes(GlobalChannelInterceptor.class.getName());
				if (CollectionUtils.isEmpty(annotationAttributes)
						&& beanDefinition.getSource() instanceof MethodMetadata) {
					MethodMetadata beanMethod = (MethodMetadata) beanDefinition.getSource();
					annotationAttributes =
							beanMethod.getAnnotationAttributes(GlobalChannelInterceptor.class.getName()); // NOSONAR not null
				}

				if (!CollectionUtils.isEmpty(annotationAttributes)) {
					Map<String, Object> attributes = annotationAttributes;
					RootBeanDefinition channelInterceptorWrapper =
							new RootBeanDefinition(GlobalChannelInterceptorWrapper.class,
									() -> createGlobalChannelInterceptorWrapper(beanName, attributes));

					BeanDefinitionReaderUtils.registerWithGeneratedName(channelInterceptorWrapper, registry);
				}
			}
		}
	}

	private GlobalChannelInterceptorWrapper createGlobalChannelInterceptorWrapper(String interceptorBeanName,
			Map<String, Object> annotationAttributes) {

		ChannelInterceptor interceptor = this.beanFactory.getBean(interceptorBeanName, ChannelInterceptor.class);
		GlobalChannelInterceptorWrapper interceptorWrapper = new GlobalChannelInterceptorWrapper(interceptor);
		String[] patterns =
				Arrays.stream((String[]) annotationAttributes.get("patterns"))
						.map(this::resolveEmbeddedValue)
						.toArray(String[]::new);
		interceptorWrapper.setPatterns(patterns);
		interceptorWrapper.setOrder((Integer) annotationAttributes.get("order"));
		return interceptorWrapper;
	}

	private String resolveEmbeddedValue(String value) {
		String valueToReturn = this.beanFactory.resolveEmbeddedValue(value);
		if (valueToReturn == null || !(valueToReturn.startsWith("#{") && value.endsWith("}"))) {
			return valueToReturn;
		}

		BeanExpressionResolver beanExpressionResolver = this.beanFactory.getBeanExpressionResolver();
		if (beanExpressionResolver != null) {
			Object result = beanExpressionResolver.evaluate(valueToReturn, this.beanExpressionContext);
			return result != null ? result.toString() : null;
		}

		return null;
	}

}
