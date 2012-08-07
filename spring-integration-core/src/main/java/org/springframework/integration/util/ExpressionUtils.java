/*
 * Copyright 2002-2012 the original author or authors.
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

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.context.expression.MapAccessor;
import org.springframework.core.convert.ConversionService;
import org.springframework.expression.BeanResolver;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.support.StandardTypeConverter;
import org.springframework.integration.context.IntegrationContextUtils;

/**
 * Utility class with static methods for helping with establishing environments for
 * SpEL expressions.
 *
 * @author Gary Russell
 * @author Oleg Zhurakousky
 * @since 2.2
 *
 */
public class ExpressionUtils {

	/**
	 * Create a {@link StandardEvaluationContext} with a {@link MapAccessor} in its
	 * property accessor property.
	 * @return the evaluation context.
	 */
	public static StandardEvaluationContext createStandardEvaluationContext() {
		return createStandardEvaluationContext(null, null);
	}

	/**
	 * Create a {@link StandardEvaluationContext} with a {@link MapAccessor} in its
	 * property accessor property and the supplied {@link BeanResolver} in its
	 * beanResolver property.
	 * @param beanResolver the bean factory.
	 * @return the evaluation context.
	 */
	public static StandardEvaluationContext createStandardEvaluationContext(BeanResolver beanResolver) {
		return createStandardEvaluationContext(beanResolver, null);
	}

	/**
	 * Create a {@link StandardEvaluationContext} with a {@link MapAccessor} in its
	 * property accessor property and the supplied {@link ConversionService} in its
	 * conversionService property.
	 * @param conversionService the conversion service.
	 * @return the evaluation context.
	 */
	public static StandardEvaluationContext createStandardEvaluationContext(ConversionService conversionService) {
		return createStandardEvaluationContext(null, conversionService);
	}

	/**
	 * Create a {@link StandardEvaluationContext} with a {@link MapAccessor} in its
	 * property accessor property, the supplied {@link BeanResolver} in its
	 * beanResolver property, and the supplied {@link ConversionService} in its
	 * conversionService property.
	 * @param beanResolver the bean factory.
	 * @param conversionService the conversion service.
	 * @return the evaluation context.
	 */
	public static StandardEvaluationContext createStandardEvaluationContext(BeanResolver beanResolver,
			ConversionService conversionService) {
		StandardEvaluationContext evaluationContext = new StandardEvaluationContext();
		evaluationContext.addPropertyAccessor(new MapAccessor());
		if (beanResolver != null) {
			evaluationContext.setBeanResolver(beanResolver);
		}
		if (conversionService != null) {
			evaluationContext.setTypeConverter(new StandardTypeConverter(conversionService));
		}
		return evaluationContext;
	}

	/**
	 * Creates {@link BeanFactoryResolver}, extracts {@link ConversionService} and delegates to
	 * {@link #createStandardEvaluationContext(BeanResolver, ConversionService)}
	 *
	 * @param beanFactory
	 * @return
	 */
	public static StandardEvaluationContext createStandardEvaluationContext(BeanFactory beanFactory) {
		ConversionService conversionService = IntegrationContextUtils.getConversionService(beanFactory);
		if (conversionService == null && beanFactory instanceof ConfigurableListableBeanFactory){
			conversionService = ((ConfigurableListableBeanFactory)beanFactory).getConversionService();
		}
		return createStandardEvaluationContext(new BeanFactoryResolver(beanFactory), conversionService);
	}
}
