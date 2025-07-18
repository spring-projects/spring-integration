/*
 * Copyright 2002-present the original author or authors.
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

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.ConversionServiceFactory;
import org.springframework.core.convert.support.GenericConversionService;
import org.springframework.integration.json.JsonNodeWrapperToJsonNodeConverter;
import org.springframework.integration.support.json.JacksonPresent;
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

	@SuppressWarnings("NullAway.Init")
	private ApplicationContext applicationContext;

	ConverterRegistrar() {
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
		Set<Object> converters =
				this.applicationContext.getBeansOfType(IntegrationConverterRegistration.class)
						.values()
						.stream().map(IntegrationConverterRegistration::converter)
						.collect(Collectors.toSet());
		if (JacksonPresent.isJackson2Present()) {
			converters.add(new JsonNodeWrapperToJsonNodeConverter());
		}
		ConversionServiceFactory.registerConverters(converters, conversionService);
	}

	/**
	 * A configuration supporting bean for converter with a {@link IntegrationConverter}
	 * annotation.
	 *
	 * @param converter the target converter bean with a {@link IntegrationConverter}.
	 *
	 * @since 6.0
	 */
	record IntegrationConverterRegistration(Object converter) {

	}

}
