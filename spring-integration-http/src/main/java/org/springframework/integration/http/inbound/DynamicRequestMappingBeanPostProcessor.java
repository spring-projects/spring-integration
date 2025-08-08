/*
 * Copyright © 2024 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2024-present the original author or authors.
 */

package org.springframework.integration.http.inbound;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.config.DestructionAwareBeanPostProcessor;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;

/**
 * A {@link DestructionAwareBeanPostProcessor} to register request mapping
 * created at runtime (e.g. via
 * {@link org.springframework.integration.dsl.context.IntegrationFlowContext})
 *  by {@link HttpRequestHandlingEndpointSupport} instances
 * into the {@link IntegrationRequestMappingHandlerMapping}.
 * These mappings are also removed when respective {@link HttpRequestHandlingEndpointSupport}
 * bean is destroyed.
 *
 * @author Artem Bilan
 *
 * @since 6.2.5
 */
public class DynamicRequestMappingBeanPostProcessor
		implements BeanFactoryAware, DestructionAwareBeanPostProcessor, SmartInitializingSingleton {

	private BeanFactory beanFactory;

	private IntegrationRequestMappingHandlerMapping integrationRequestMappingHandlerMapping;

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}

	@Override
	public void afterSingletonsInstantiated() {
		this.integrationRequestMappingHandlerMapping =
				this.beanFactory.getBean(IntegrationRequestMappingHandlerMapping.class);
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if (this.integrationRequestMappingHandlerMapping != null && isHandler(bean.getClass())) {
			this.integrationRequestMappingHandlerMapping.detectHandlerMethods(bean);
		}
		return bean;
	}

	@Override
	public void postProcessBeforeDestruction(Object bean, String beanName) throws BeansException {
		if (isHandler(bean.getClass())) {
			RequestMappingInfo mapping =
					this.integrationRequestMappingHandlerMapping.getMappingForEndpoint((BaseHttpInboundEndpoint) bean);
			if (mapping != null) {
				this.integrationRequestMappingHandlerMapping.unregisterMapping(mapping);
			}
		}
	}

	@Override
	public boolean requiresDestruction(Object bean) {
		return isHandler(bean.getClass());
	}

	private boolean isHandler(Class<?> beanType) {
		return HttpRequestHandlingEndpointSupport.class.isAssignableFrom(beanType);
	}

}
