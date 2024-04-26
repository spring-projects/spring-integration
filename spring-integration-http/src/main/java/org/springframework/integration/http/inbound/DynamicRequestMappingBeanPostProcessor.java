/*
 * Copyright 2024 the original author or authors.
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
