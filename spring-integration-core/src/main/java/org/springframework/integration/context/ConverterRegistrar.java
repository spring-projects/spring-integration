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

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.InitializingBean;
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
 * @since 2.0
 */
class ConverterRegistrar implements InitializingBean, BeanFactoryAware {

	private final Set<?> converters;

	private BeanFactory beanFactory;


	public ConverterRegistrar(Set<?> converters) {
		this.converters = converters;
	}


	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		Assert.notNull(beanFactory, "BeanFactory is required");
		ConversionService conversionService = IntegrationUtils.getConversionService(beanFactory);
		if (conversionService instanceof GenericConversionService) {
			ConversionServiceFactory.registerConverters(converters, (GenericConversionService) conversionService);
		}
		else {
			Assert.notNull(conversionService, "Failed to locate '" + IntegrationUtils.INTEGRATION_CONVERSION_SERVICE_BEAN_NAME + "'");
		}
	}

}
