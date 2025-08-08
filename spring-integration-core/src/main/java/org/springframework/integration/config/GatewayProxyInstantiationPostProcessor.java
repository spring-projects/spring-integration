/*
 * Copyright © 2022 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2022-present the original author or authors.
 */

package org.springframework.integration.config;

import org.springframework.aop.framework.AopInfrastructureBean;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.AnnotatedGenericBeanDefinition;
import org.springframework.beans.factory.aot.BeanRegistrationAotContribution;
import org.springframework.beans.factory.aot.BeanRegistrationAotProcessor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.InstantiationAwareBeanPostProcessor;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.RegisteredBean;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.annotation.ScannedGenericBeanDefinition;
import org.springframework.core.ResolvableType;
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
		InstantiationAwareBeanPostProcessor, BeanRegistrationAotProcessor, ApplicationContextAware, AopInfrastructureBean {

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
		if (beanClass.isInterface() && AnnotatedElementUtils.hasAnnotation(beanClass, MessagingGateway.class)) {
			BeanDefinition beanDefinition = this.registry.getBeanDefinition(beanName);
			if (beanDefinition instanceof AnnotatedGenericBeanDefinition
					|| beanDefinition instanceof ScannedGenericBeanDefinition) {

				AnnotationGatewayProxyFactoryBean<?> gatewayProxyFactoryBean =
						new AnnotationGatewayProxyFactoryBean<>(beanClass);
				gatewayProxyFactoryBean.setApplicationContext(this.applicationContext);
				gatewayProxyFactoryBean.setBeanFactory(this.applicationContext.getAutowireCapableBeanFactory());
				ClassLoader classLoader = this.applicationContext.getClassLoader();
				if (classLoader != null) {
					gatewayProxyFactoryBean.setBeanClassLoader(classLoader);
				}
				gatewayProxyFactoryBean.setBeanName(beanName);
				gatewayProxyFactoryBean.afterPropertiesSet();
				return gatewayProxyFactoryBean;
			}
		}
		return null;
	}

	@Override
	public BeanRegistrationAotContribution processAheadOfTime(RegisteredBean registeredBean) {
		Class<?> beanClass = registeredBean.getBeanClass();
		if (beanClass.isInterface() && AnnotatedElementUtils.hasAnnotation(beanClass, MessagingGateway.class)) {
			RootBeanDefinition beanDefinition = registeredBean.getMergedBeanDefinition();
			beanDefinition.setBeanClass(AnnotationGatewayProxyFactoryBean.class);
			beanDefinition.getConstructorArgumentValues().addIndexedArgumentValue(0, beanClass);
			beanDefinition.setTargetType(
					ResolvableType.forClassWithGenerics(AnnotationGatewayProxyFactoryBean.class, beanClass));
		}
		return null;
	}

}
