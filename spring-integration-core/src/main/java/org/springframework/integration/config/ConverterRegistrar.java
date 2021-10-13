/*
 * Copyright 2002-2021 the original author or authors.
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

import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.ConversionServiceFactory;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.util.Assert;

/**
 * Utility class that keeps track of a set of Converters in order to register
 * them with the "integrationConversionService" upon initialization.
 *
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 2.0
 */
class ConverterRegistrar implements InitializingBean, ApplicationContextAware {

	private final Set<Object> converters;

	private ApplicationContext applicationContext;


	ConverterRegistrar() {
		this(new HashSet<>());
	}

	ConverterRegistrar(Set<Object> converters) {
		this.converters = converters;
	}

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public void afterPropertiesSet() {
		ConversionService conversionService = IntegrationUtils.getConversionService(this.applicationContext);
		if (conversionService instanceof GenericConversionService) {
			registerConverters((GenericConversionService) conversionService);
		}
		else {
			Assert.notNull(conversionService,
					() -> "Failed to locate '" + IntegrationUtils.INTEGRATION_CONVERSION_SERVICE_BEAN_NAME + "'");
		}
	}

	private void registerConverters(GenericConversionService conversionService) {
		this.converters.addAll(this.applicationContext.getBeansWithAnnotation(IntegrationConverter.class).values());
		ConversionServiceFactory.registerConverters(this.converters, conversionService);
	}

}
