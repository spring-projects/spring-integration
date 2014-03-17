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

import java.util.List;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.core.type.StandardMethodMetadata;
import org.springframework.integration.channel.interceptor.GlobalChannelInterceptorBeanPostProcessor;
import org.springframework.integration.channel.interceptor.GlobalChannelInterceptorWrapper;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.messaging.support.ChannelInterceptor;

/**
 * The {@link IntegrationConfigurationInitializer} to populate {@link GlobalChannelInterceptorWrapper}
 * for {@link ChannelInterceptor}s marked with {@link GlobalChannelInterceptor} annotation.
 * <p>
 * {@link org.springframework.context.annotation.Bean} methods are also processed.
 * <p>
 * In addition this component registers {@link GlobalChannelInterceptorBeanPostProcessor} if necessary.
 *
 * @author Artem Bilan
 * @since 4.0
 */
public class GlobalChannelInterceptorInitializer implements IntegrationConfigurationInitializer {

	@Override
	public void initialize(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;

		List<GlobalChannelInterceptorWrapper> globalChannelInterceptors = new ManagedList<GlobalChannelInterceptorWrapper>();

		Map<String, ChannelInterceptor> beansOfType = beanFactory.getBeansOfType(ChannelInterceptor.class);

		for (Map.Entry<String, ChannelInterceptor> entry : beansOfType.entrySet()) {
			String beanName = entry.getKey();
			ChannelInterceptor interceptor = entry.getValue();
			GlobalChannelInterceptor annotation = AnnotationUtils.findAnnotation(interceptor.getClass(), GlobalChannelInterceptor.class);
			if (annotation == null) {
				BeanDefinition beanDefinition = registry.getBeanDefinition(beanName);
				if (beanDefinition instanceof AnnotatedBeanDefinition) {
					StandardMethodMetadata beanMethod = (StandardMethodMetadata) beanDefinition.getSource();
					annotation = AnnotationUtils.findAnnotation(beanMethod.getIntrospectedMethod(), GlobalChannelInterceptor.class);
				}
			}
			if (annotation != null) {
				GlobalChannelInterceptorWrapper wrapper = new GlobalChannelInterceptorWrapper(interceptor);
				wrapper.setPatterns(annotation.patterns());
				wrapper.setOrder(annotation.order());
				globalChannelInterceptors.add(wrapper);
			}
		}

		if (beanFactory.containsBeanDefinition(IntegrationContextUtils.GLOBAL_CHANNEL_INTERCEPTOR_POST_PROCESSOR_BEAN_NAME)) {
			BeanDefinition postProcessorDef = registry.getBeanDefinition(IntegrationContextUtils.GLOBAL_CHANNEL_INTERCEPTOR_POST_PROCESSOR_BEAN_NAME);
			@SuppressWarnings("unchecked")
			List<Object> globalChannelInterceptorsValue = (List<Object>) postProcessorDef
					.getConstructorArgumentValues().getIndexedArgumentValues().values().iterator().next().getValue();
			globalChannelInterceptorsValue.addAll(globalChannelInterceptors);
		}
		else {
			BeanDefinitionBuilder postProcessorBuilder = BeanDefinitionBuilder.genericBeanDefinition(GlobalChannelInterceptorBeanPostProcessor.class);
			postProcessorBuilder.addConstructorArgValue(globalChannelInterceptors);
			BeanDefinition beanDef = postProcessorBuilder.getBeanDefinition();
			registry.registerBeanDefinition(IntegrationContextUtils.GLOBAL_CHANNEL_INTERCEPTOR_POST_PROCESSOR_BEAN_NAME, beanDef);
		}

	}

}
