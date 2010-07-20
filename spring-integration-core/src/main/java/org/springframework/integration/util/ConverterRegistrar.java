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
package org.springframework.integration.util;

import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.context.support.ConversionServiceFactoryBean;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;
import org.springframework.core.convert.support.ConversionServiceFactory;
import org.springframework.core.convert.support.GenericConversionService;

/**
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class ConverterRegistrar implements BeanFactoryPostProcessor {
	private final Set<Converter<?, ?>> converters;
	
	public ConverterRegistrar(Set<Converter<?, ?>> converters){
		this.converters = converters;
	}
	/**
	 * This method will add converters to the SI's conversion service - {@link SI#CONVERSION_SERVICE}
	 * If SI's conversion service does not exist, then an instance of the {@link ConversionService} will
	 * be created and registered under the name {@link SI#CONVERSION_SERVICE}
	 */
	public void postProcessBeanFactory(
			ConfigurableListableBeanFactory beanFactory) throws BeansException {
		if (!beanFactory.containsBean(SI.CONVERSION_SERVICE)){
			BeanDefinitionBuilder conversionServiceBuilder = BeanDefinitionBuilder.rootBeanDefinition(ConversionServiceFactoryBean.class);
			BeanDefinitionHolder csHolder = new BeanDefinitionHolder(conversionServiceBuilder.getBeanDefinition(), SI.CONVERSION_SERVICE);
			BeanDefinitionReaderUtils.registerBeanDefinition(csHolder, (BeanDefinitionRegistry) beanFactory);
		}
		GenericConversionService conversionService = beanFactory.getBean(SI.CONVERSION_SERVICE, GenericConversionService.class);
		ConversionServiceFactory.registerConverters(converters, conversionService);
	}
}
