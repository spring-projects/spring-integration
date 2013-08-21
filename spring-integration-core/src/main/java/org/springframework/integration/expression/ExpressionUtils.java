/*
 * Copyright 2002-2013 the original author or authors.
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

import java.util.Collection;
import java.util.Collections;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.ListableBeanFactory;
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
 * @author Artem Bilan
 * @since 2.2
 */
public abstract class ExpressionUtils {

	/**
	 * Create a {@link StandardEvaluationContext} with a {@link MapAccessor} in its
	 * property accessor property.
	 * @return the evaluation context.
	 */
	public static StandardEvaluationContext createStandardEvaluationContext() {
		return createStandardEvaluationContext((BeanResolver) null, null);
	}

	/**
	 * Create a {@link StandardEvaluationContext} with a {@link MapAccessor} in its
	 * property accessor property and the supplied {@link BeanResolver} in its
	 * beanResolver property.
	 * @param beanResolver the bean resolver.
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
		return createStandardEvaluationContext((BeanResolver) null, conversionService);
	}

	/**
	 * Create a {@link StandardEvaluationContext} with a {@link MapAccessor} in its
	 * property accessor property, the supplied {@link BeanResolver} in its
	 * beanResolver property, and the supplied {@link ConversionService} in its
	 * conversionService property.
	 * @param beanResolver the bean resolver.
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

	public static StandardEvaluationContext createStandardEvaluationContext(BeanFactory beanFactory,
			ConversionService conversionService) {
		StandardEvaluationContext context = null;
		if (beanFactory != null) {
			context = IntegrationContextUtils.getEvaluationContext(beanFactory);
			if (context != null) {
				configureSpelFunctions(context, beanFactory);
				if (conversionService != null) {
					context.setTypeConverter(new StandardTypeConverter(conversionService));
				}
			}
		}
		 if (context == null) {
			 context = createStandardEvaluationContext(conversionService);
		 }

		return context;
	}

	public static StandardEvaluationContext createStandardEvaluationContext(BeanFactory beanFactory) {
		return createStandardEvaluationContext(beanFactory, IntegrationContextUtils.getConversionService(beanFactory));
	}

	public static Collection<SpelFunction> getSpelFunctions(BeanFactory beanFactory) {
		if (beanFactory instanceof ListableBeanFactory) {
			return ((ListableBeanFactory) beanFactory).getBeansOfType(SpelFunction.class).values();
		}
		return Collections.emptyList();
	}

	public static void configureSpelFunctions(StandardEvaluationContext context, BeanFactory beanFactory) {
		Collection<SpelFunction> functions = getSpelFunctions(beanFactory);
		for (SpelFunction function : functions) {
			context.registerFunction(function.getName(), function.getMethod());
		}
	}

}
