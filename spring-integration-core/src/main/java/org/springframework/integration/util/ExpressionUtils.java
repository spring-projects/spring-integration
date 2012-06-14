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
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.context.expression.MapAccessor;
import org.springframework.core.convert.ConversionService;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.support.StandardTypeConverter;

/**
 * @author Gary Russell
 * @since 2.2
 *
 */
public class ExpressionUtils {

	public static StandardEvaluationContext createStandardEvaluationContext() {
		return createStandardEvaluationContext(null, null);
	}

	public static StandardEvaluationContext createStandardEvaluationContext(BeanFactory beanFactory) {
		return createStandardEvaluationContext(beanFactory, null);
	}

	public static StandardEvaluationContext createStandardEvaluationContext(ConversionService conversionService) {
		return createStandardEvaluationContext(null, conversionService);
	}

	public static StandardEvaluationContext createStandardEvaluationContext(BeanFactory beanFactory,
			ConversionService conversionService) {
		StandardEvaluationContext evaluationContext = new StandardEvaluationContext();
		evaluationContext.addPropertyAccessor(new MapAccessor());
		if (beanFactory != null) {
			evaluationContext.setBeanResolver(new BeanFactoryResolver(beanFactory));
		}
		if (conversionService != null) {
			evaluationContext.setTypeConverter(new StandardTypeConverter(conversionService));
		}
		return evaluationContext;
	}
}
