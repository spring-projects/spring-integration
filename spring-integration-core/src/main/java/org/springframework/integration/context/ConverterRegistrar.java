/*
 * Copyright 2002-2010 the original author or authors.
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
package org.springframework.integration.context;

import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.ConversionServiceFactory;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.util.Assert;

/**
 * @author Oleg Zhurakousky
 * @since 2.0
 */
class ConverterRegistrar implements InitializingBean, BeanFactoryAware {
	private final Set<Converter<?, ?>> converters;
	private BeanFactory beanFactory;
	
	public ConverterRegistrar(Set<Converter<?, ?>> converters){
		this.converters = converters;
	}

	public void afterPropertiesSet() throws Exception {
		GenericConversionService conversionService = beanFactory.getBean(IntegrationContextUtils.INTEGRATION_CONVERSION_SERVICE_BEAN_NAME, GenericConversionService.class);
		Assert.notNull(conversionService, "can not locate '" + IntegrationContextUtils.INTEGRATION_CONVERSION_SERVICE_BEAN_NAME + "' ");
		ConversionServiceFactory.registerConverters(converters, conversionService);
	}

	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		this.beanFactory = beanFactory;
	}
	
}
