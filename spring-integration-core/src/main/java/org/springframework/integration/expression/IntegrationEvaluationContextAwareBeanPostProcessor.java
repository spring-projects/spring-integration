/*
 * Copyright 2013 the original author or authors.
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

package org.springframework.integration.expression;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.core.convert.ConversionService;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.support.StandardTypeConverter;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.context.IntegrationObjectSupport;

/**
 * @author Artem Bilan
 * @since 3.0
 */
public class IntegrationEvaluationContextAwareBeanPostProcessor implements BeanPostProcessor {

	private final BeanFactory beanFactory;

	public IntegrationEvaluationContextAwareBeanPostProcessor(BeanFactory beanFactory) {
		this.beanFactory = beanFactory;
	}

	@Override
	public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
		if (bean instanceof IntegrationEvaluationContextAware) {
			StandardEvaluationContext evaluationContext = IntegrationContextUtils.getEvaluationContext(beanFactory);
			if (bean instanceof IntegrationObjectSupport) {
				ConversionService conversionService = ((IntegrationObjectSupport) bean).getConversionService();
				if (conversionService != null) {
					evaluationContext.setTypeConverter(new StandardTypeConverter(conversionService));
				}
			}
			((IntegrationEvaluationContextAware) bean).setIntegrationEvaluationContext(evaluationContext);
		}
		return bean;
	}

	@Override
	public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
		return bean;
	}
}
