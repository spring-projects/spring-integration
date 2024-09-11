/*
 * Copyright 2018-2024 the original author or authors.
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
import java.util.Collection;
import java.util.Map.Entry;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.IndexAccessor;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.spel.support.DataBindingPropertyAccessor;
import org.springframework.expression.spel.support.SimpleEvaluationContext;
import org.springframework.integration.context.IntegrationContextUtils;

/**
 * <p>
 * {@link FactoryBean} to populate {@link SimpleEvaluationContext} instances enhanced with:
 * <ul>
 * <li>
 * a {@link org.springframework.expression.TypeConverter} based on the
 * {@link org.springframework.core.convert.ConversionService}
 * from the application context.
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
 * @since 4.3.15
 */
public class IntegrationSimpleEvaluationContextFactoryBean extends AbstractEvaluationContextFactoryBean
		implements FactoryBean<SimpleEvaluationContext> {

	@Override
	public boolean isSingleton() {
		return false;
	}

	@Override
	public void afterPropertiesSet() {
		initialize(IntegrationContextUtils.INTEGRATION_SIMPLE_EVALUATION_CONTEXT_BEAN_NAME);
	}

	@Override
	public SimpleEvaluationContext getObject() {
		Collection<PropertyAccessor> accessors = getPropertyAccessors().values();
		PropertyAccessor[] accessorArray = accessors.toArray(new PropertyAccessor[accessors.size() + 2]);
		accessorArray[accessors.size()] = new MapAccessor();
		accessorArray[accessors.size() + 1] = DataBindingPropertyAccessor.forReadOnlyAccess();
		SimpleEvaluationContext evaluationContext =
				SimpleEvaluationContext.forPropertyAccessors(accessorArray)
						.withIndexAccessors(getIndexAccessors().values().toArray(new IndexAccessor[0]))
						.withTypeConverter(getTypeConverter())
						.withInstanceMethods()
						.withAssignmentDisabled()
						.build();
		for (Entry<String, Method> functionEntry : getFunctions().entrySet()) {
			evaluationContext.setVariable(functionEntry.getKey(), functionEntry.getValue());
		}
		return evaluationContext;
	}

	@Override
	public Class<?> getObjectType() {
		return SimpleEvaluationContext.class;
	}

}
