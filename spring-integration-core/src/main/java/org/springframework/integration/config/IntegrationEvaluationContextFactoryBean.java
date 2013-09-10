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

package org.springframework.integration.config;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
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
import org.springframework.util.Assert;

/**
 * {@link FactoryBean} to populate {@link StandardEvaluationContext} instances enhanced with:
 * <ul>
 * <li>
 * a {@link BeanFactoryResolver}.
 * </li>
 * <li>
 * a {@link TypeConverter} based on the {@link ConversionService} from the application context.
 * </li>
 * <li>
 * a set of provided {@link PropertyAccessor}s including a default {@link MapAccessor}.
 * </li>
 * <li>
 * a set of provided SpEL functions.
 * </li>
 * </ul>
 * <p/>
 * This factory returns a new instance for each reference singleton - {@link #isSingleton()}
 * returns false.
 *
 * @author Artem Bilan
 * @author Gary Russell
 * @since 3.0
 */
public class IntegrationEvaluationContextFactoryBean implements FactoryBean<StandardEvaluationContext>,
		ApplicationContextAware, InitializingBean {

	private volatile List<PropertyAccessor> propertyAccessors = new ArrayList<PropertyAccessor>();

	private volatile Map<String, Method> functions = new LinkedHashMap<String, Method>();

	private TypeConverter typeConverter = new StandardTypeConverter();

	private ApplicationContext applicationContext;

	private BeanResolver beanResolver;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	public void setPropertyAccessors(PropertyAccessor... accessors) {
		Assert.noNullElements(accessors, "Cannot have null elements in accessors");
		List<PropertyAccessor> propertyAccessors = new ArrayList<PropertyAccessor>();
		loadDefaultPropertyAccessors(propertyAccessors);
		Collections.addAll(propertyAccessors, accessors);
		this.propertyAccessors = propertyAccessors;
	}

	public void setFunctions(Map<String, Method> functionsArg) {
		Map<String, Method> functions = new LinkedHashMap<String, Method>();
		for (Entry<String, Method> function : functionsArg.entrySet()) {
			Assert.notNull(function.getValue(), "Method cannot be null");
			functions.put(function.getKey(), function.getValue());
		}
		this.functions = functions;
	}


	@Override
	public void afterPropertiesSet() throws Exception {
		if (this.propertyAccessors.isEmpty()) {
			this.loadDefaultPropertyAccessors(this.propertyAccessors);
		}
		if (this.applicationContext != null) {
			this.beanResolver = new BeanFactoryResolver(this.applicationContext);
			ConversionService conversionService = IntegrationContextUtils.getConversionService(this.applicationContext);
			if (conversionService != null) {
				this.typeConverter = new StandardTypeConverter(conversionService);
			}

			Map<String, SpelFunctionFactoryBean> functionFactoryBeanMap =
					BeanFactoryUtils.beansOfTypeIncludingAncestors(this.applicationContext, SpelFunctionFactoryBean.class);
			for (SpelFunctionFactoryBean spelFunctionFactoryBean : functionFactoryBeanMap.values()) {
				if (!this.functions.containsKey(spelFunctionFactoryBean.getFunctionName())) {
					this.functions.put(spelFunctionFactoryBean.getFunctionName(), spelFunctionFactoryBean.getObject());
				}
			}
		}
	}

	@Override
	public StandardEvaluationContext getObject() throws Exception {
		StandardEvaluationContext evaluationContext = new StandardEvaluationContext();

		evaluationContext.setBeanResolver(this.beanResolver);
		evaluationContext.setTypeConverter(this.typeConverter);

		for (PropertyAccessor propertyAccessor : this.propertyAccessors) {
			evaluationContext.addPropertyAccessor(propertyAccessor);
		}

		for (Entry<String, Method> functionEntry : this.functions.entrySet()) {
			evaluationContext.registerFunction(functionEntry.getKey(), functionEntry.getValue());
		}

		return evaluationContext;
	}

	private void loadDefaultPropertyAccessors(List<PropertyAccessor> propertyAccessors) {
		propertyAccessors.add(new MapAccessor());
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
