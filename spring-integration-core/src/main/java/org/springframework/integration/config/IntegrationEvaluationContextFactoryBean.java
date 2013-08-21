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

package org.springframework.integration.config;

import java.util.Collection;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.context.expression.MapAccessor;
import org.springframework.core.convert.ConversionService;
import org.springframework.expression.BeanResolver;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypeConverter;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.support.StandardTypeConverter;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.expression.ExpressionUtils;
import org.springframework.integration.expression.SpelFunction;
import org.springframework.integration.util.BeanFactoryTypeConverter;

/**
 * @author Artem Bilan
 * @since 3.0
 */
public class IntegrationEvaluationContextFactoryBean implements FactoryBean<StandardEvaluationContext>,
		ApplicationContextAware, InitializingBean {

	private final PropertyAccessor propertyAccessor = new MapAccessor();

	private TypeConverter typeConverter = new StandardTypeConverter();

	private Collection<SpelFunction> functions;

	private ApplicationContext applicationContext;

	private BeanResolver beanResolver;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	@Override
	public StandardEvaluationContext getObject() throws Exception {
		StandardEvaluationContext evaluationContext = new StandardEvaluationContext();
		evaluationContext.addPropertyAccessor(this.propertyAccessor);
		evaluationContext.setBeanResolver(beanResolver);
		evaluationContext.setTypeConverter(typeConverter);
		for (SpelFunction function : functions) {
			evaluationContext.registerFunction(function.getName(), function.getMethod());
		}
		return evaluationContext;
	}

	@Override
	public void afterPropertiesSet() throws Exception {
		this.beanResolver = new BeanFactoryResolver(this.applicationContext);
		this.functions = ExpressionUtils.getSpelFunctions(this.applicationContext);
		ConversionService conversionService = IntegrationContextUtils.getConversionService(this.applicationContext);
		if (conversionService != null) {
			this.typeConverter = new StandardTypeConverter(conversionService);
		}
	}

	@Override
	public Class<?> getObjectType() {
		return StandardEvaluationContext.class;
	}

	@Override
	public boolean isSingleton() {
		return false;
	}

}
