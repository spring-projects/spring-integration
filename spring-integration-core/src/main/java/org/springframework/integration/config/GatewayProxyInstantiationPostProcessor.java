/*
 * Copyright 2022 the original author or authors.
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

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.gateway.AnnotationGatewayProxyFactoryBean;

/**
 * The {@link InstantiationAwareBeanPostProcessor} to wrap beans for {@link MessagingGateway}
 * into {@link AnnotationGatewayProxyFactoryBean}.
 *
 * @author Artem Bilan
 *
 * @since 6.0
 *
 * @see AnnotationGatewayProxyFactoryBean
 */
class GatewayProxyInstantiationPostProcessor implements
		InstantiationAwareBeanPostProcessor, ApplicationContextAware {

	private final BeanDefinitionRegistry registry;

	private ApplicationContext applicationContext;

	GatewayProxyInstantiationPostProcessor(BeanDefinitionRegistry registry) {
		this.registry = registry;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public Object postProcessBeforeInstantiation(Class<?> beanClass, String beanName) throws BeansException {
		if (beanClass.isInterface()
				&& AnnotatedElementUtils.hasAnnotation(beanClass, MessagingGateway.class)
				&& this.registry.getBeanDefinition(beanName) instanceof AnnotatedGenericBeanDefinition) {

			AnnotationGatewayProxyFactoryBean<?> gatewayProxyFactoryBean =
					new AnnotationGatewayProxyFactoryBean<>(beanClass);
			gatewayProxyFactoryBean.setApplicationContext(this.applicationContext);
			gatewayProxyFactoryBean.setBeanFactory(this.applicationContext.getAutowireCapableBeanFactory());
			ClassLoader classLoader = this.applicationContext.getClassLoader();
			if (classLoader != null) {
				gatewayProxyFactoryBean.setBeanClassLoader(classLoader);
			}
			gatewayProxyFactoryBean.setBeanName(beanName);
			return gatewayProxyFactoryBean;
		}
		return null;
	}

}
