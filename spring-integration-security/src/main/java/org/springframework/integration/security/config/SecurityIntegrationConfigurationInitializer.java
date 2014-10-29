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

package org.springframework.integration.security.config;

import java.lang.reflect.Method;
import java.util.List;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.CannotLoadBeanClassException;
import org.springframework.beans.factory.annotation.AnnotatedBeanDefinition;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.core.type.MethodMetadata;
import org.springframework.core.type.StandardMethodMetadata;
import org.springframework.integration.config.IntegrationConfigurationInitializer;
import org.springframework.integration.security.channel.ChannelSecurityInterceptor;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * The Integration Security infrastructure {@code beanFactory} initializer.
 *
 * @author Artem Bilan
 * @since 4.0
 */
public class SecurityIntegrationConfigurationInitializer implements IntegrationConfigurationInitializer {

	private static final String CHANNEL_SECURITY_INTERCEPTOR_BPP_BEAN_NAME =
			ChannelSecurityInterceptorBeanPostProcessor.class.getName();

	@Override
	public void initialize(ConfigurableListableBeanFactory beanFactory) throws BeansException {
		BeanDefinitionRegistry registry = (BeanDefinitionRegistry) beanFactory;

		List<BeanDefinition> securityInterceptors = new ManagedList<BeanDefinition>();

		for (String beanName : registry.getBeanDefinitionNames()) {
			BeanDefinition beanDefinition = registry.getBeanDefinition(beanName);
			String beanClassName = beanDefinition.getBeanClassName();
			Class<?> clazz = null;
			if (StringUtils.hasText(beanClassName)) {
				try {
					clazz = ClassUtils.forName(beanClassName, beanFactory.getBeanClassLoader());
				}
				catch (ClassNotFoundException e) {
					throw new CannotLoadBeanClassException(this.toString(), beanName, beanClassName, e);
				}
			}
			else if (beanDefinition instanceof AnnotatedBeanDefinition
					&& beanDefinition.getSource() instanceof MethodMetadata) {
				MethodMetadata beanMethod = (MethodMetadata) beanDefinition.getSource();
				if (beanMethod instanceof StandardMethodMetadata) {
					Method method = ((StandardMethodMetadata) beanMethod).getIntrospectedMethod();
					clazz = method.getReturnType();
				}
			}

			if (clazz != null &&
					(ChannelSecurityInterceptor.class.isAssignableFrom(clazz)
							|| ChannelSecurityInterceptorFactoryBean.class.isAssignableFrom(clazz))) {
				securityInterceptors.add(beanDefinition);
			}
		}

		if (!securityInterceptors.isEmpty()) {
			BeanDefinition securityPostProcessorBd =
					BeanDefinitionBuilder.rootBeanDefinition(ChannelSecurityInterceptorBeanPostProcessor.class)
							.addConstructorArgValue(securityInterceptors)
							.getBeanDefinition();
			registry.registerBeanDefinition(CHANNEL_SECURITY_INTERCEPTOR_BPP_BEAN_NAME, securityPostProcessorBd);
		}
	}

}
