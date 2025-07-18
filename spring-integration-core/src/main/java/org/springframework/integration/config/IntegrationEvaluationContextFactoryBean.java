/*
 * Copyright 2013-present the original author or authors.
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

import java.lang.reflect.Method;
import java.util.Map.Entry;

import org.jspecify.annotations.Nullable;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.BeanResolver;
import org.springframework.expression.IndexAccessor;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypeLocator;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.support.StandardTypeLocator;
import org.springframework.integration.context.IntegrationContextUtils;

/**
 * <p>
 * {@link FactoryBean} to populate {@link StandardEvaluationContext} instances enhanced with:
 * <ul>
 * <li>
 * a {@link BeanFactoryResolver}.
 * </li>
 * <li>
 * a {@link org.springframework.expression.TypeConverter} based on the
 * {@link org.springframework.core.convert.ConversionService} from the application context.
 * </li>
 * <li>
 * a set of provided {@link PropertyAccessor}s including a default {@link MapAccessor}.
 * </li>
 * <li>
 * a set of provided SpEL functions.
 * </li>
 * </ul>
 * <p>
 * After initialization this factory populates functions and property accessors from
 * {@link SpelFunctionFactoryBean}s and
 * {@link org.springframework.integration.expression.SpelPropertyAccessorRegistrar},
 * respectively.
 * Functions and property accessors are also inherited from any parent context.
 * </p>
 * <p>
 * This factory returns a new instance for each reference - {@link #isSingleton()} returns false.
 * </p>
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 3.0
 */
public class IntegrationEvaluationContextFactoryBean extends AbstractEvaluationContextFactoryBean
		implements FactoryBean<StandardEvaluationContext> {

	@Nullable
	private TypeLocator typeLocator;

	@SuppressWarnings("NullAway.Init")
	private BeanResolver beanResolver;

	public void setTypeLocator(TypeLocator typeLocator) {
		this.typeLocator = typeLocator;
	}

	@Override
	public boolean isSingleton() {
		return false;
	}

	@Override
	public void afterPropertiesSet() {
		ApplicationContext applicationContext = getApplicationContext();
		if (applicationContext != null) {
			this.beanResolver = new BeanFactoryResolver(applicationContext);
			if (this.typeLocator == null) {
				this.typeLocator = new StandardTypeLocator(applicationContext.getClassLoader());
			}
		}
		initialize(IntegrationContextUtils.INTEGRATION_EVALUATION_CONTEXT_BEAN_NAME);
	}

	@Override
	public StandardEvaluationContext getObject() {
		StandardEvaluationContext evaluationContext = new StandardEvaluationContext();
		if (this.typeLocator != null) {
			evaluationContext.setTypeLocator(this.typeLocator);
		}

		evaluationContext.setBeanResolver(this.beanResolver);
		evaluationContext.setTypeConverter(getTypeConverter());

		for (PropertyAccessor propertyAccessor : getPropertyAccessors().values()) {
			evaluationContext.addPropertyAccessor(propertyAccessor);
		}

		evaluationContext.addPropertyAccessor(new MapAccessor());

		for (IndexAccessor indexAccessor : getIndexAccessors().values()) {
			evaluationContext.addIndexAccessor(indexAccessor);
		}

		for (Entry<String, Method> functionEntry : getFunctions().entrySet()) {
			evaluationContext.registerFunction(functionEntry.getKey(), functionEntry.getValue());
		}

		return evaluationContext;
	}

	@Override
	public Class<?> getObjectType() {
		return StandardEvaluationContext.class;
	}

}
