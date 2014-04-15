/*
 * Copyright 2014 the original author or authors.
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
package org.springframework.integration.support.converter;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.converter.MessageConverter;
import org.springframework.util.Assert;

/**
 * Default message converter for datatype channels. Registered under bean name
 * 'datatypeChannelMessageConverter'. Delegates to the 'integrationConversionService',
 * if present.
 *
 * @author Gary Russell
 * @since 4.0
 *
 */
public class DefaultDatatypeChannelMessageConverter implements MessageConverter,
		BeanFactoryAware {

	private volatile ConversionService conversionService = new DefaultConversionService();

	private volatile boolean conversionServiceSet;

	/**
	 * Specify the {@link ConversionService} to use when trying to convert to
	 * requested type. If this property is not set explicitly but
	 * the converter is managed within a context, it will attempt to locate a
	 * bean named "integrationConversionService" defined within that context.
	 *
	 * @param conversionService The conversion service.
	 */
	public void setConversionService(ConversionService conversionService) {
		Assert.notNull(conversionService, "'conversionService' must not be null");
		this.conversionService = conversionService;
		this.conversionServiceSet = true;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		if (!this.conversionServiceSet && beanFactory != null) {
			this.conversionService = IntegrationUtils.getConversionService(beanFactory);
		}
	}

	/**
	 * @return the converted payload or null if conversion is not possible.
	 */
	@Override
	public Object fromMessage(Message<?> message, Class<?> targetClass) {
		ConversionService conversionService = this.conversionService;
		if (conversionService != null) {
			if (conversionService.canConvert(message.getPayload().getClass(), targetClass)) {
				return conversionService.convert(message.getPayload(), targetClass);
			}
		}
		return null;
	}

	@Override
	public Message<?> toMessage(Object payload, MessageHeaders header) {
		throw new UnsupportedOperationException("This converter does not support this method");
	}

}
