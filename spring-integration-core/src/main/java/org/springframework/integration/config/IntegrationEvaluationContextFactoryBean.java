/*
 * Copyright 2013-2014 the original author or authors.
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
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactoryUtils;
import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.expression.BeanFactoryResolver;
import org.springframework.context.expression.MapAccessor;
import org.springframework.core.Ordered;
import org.springframework.core.convert.ConversionService;
import org.springframework.expression.BeanResolver;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.TypeConverter;
import org.springframework.expression.TypeLocator;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.expression.spel.support.StandardTypeConverter;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.support.CompositePropertyAccessor;
import org.springframework.integration.support.utils.IntegrationUtils;
import org.springframework.util.Assert;

/**
 * <p>
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
 * <p>
 * After initialization this factory populates functions and property accessors from
 * {@link SpelFunctionFactoryBean}s and {@link SpelPropertyAccessorRegistrar}, respectively.
 * Functions and property accessors are also inherited from any parent context.
 * </p>
 * <p>
 * This factory returns a new instance for each reference - {@link #isSingleton()} returns false.
 * </p>
 *
 * @author Artem Bilan
 * @author Gary Russell
 * @since 3.0
 */
public class IntegrationEvaluationContextFactoryBean implements FactoryBean<StandardEvaluationContext>,
		ApplicationContextAware, InitializingBean {

	private final MapAccessor mapAccessor = new MapAccessor();

	private volatile Map<String, PropertyAccessor> propertyAccessors = new LinkedHashMap<String, PropertyAccessor>();

	private volatile Map<String, Method> functions = new LinkedHashMap<String, Method>();

	private TypeConverter typeConverter = new StandardTypeConverter();

	private volatile TypeLocator typeLocator;

	private BeanResolver beanResolver;

	private ApplicationContext applicationContext;

	private volatile boolean initialized;

	@Override
	public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
		this.applicationContext = applicationContext;
	}

	public void setPropertyAccessors(Map<String, PropertyAccessor> accessors) {
		Assert.isTrue(!this.initialized, "'propertyAccessors' can't be changed after initialization.");
		Assert.notNull(accessors, "'accessors' must not be null.");
		Assert.noNullElements(accessors.values().toArray(), "'accessors' cannot have null values.");
		this.propertyAccessors = new LinkedHashMap<String, PropertyAccessor>(accessors);
	}

	public Map<String, PropertyAccessor> getPropertyAccessors() {
		return propertyAccessors;
	}

	public void setFunctions(Map<String, Method> functionsArg) {
		Assert.isTrue(!this.initialized, "'functions' can't be changed after initialization.");
		Assert.notNull(functionsArg, "'functions' must not be null.");
		Assert.noNullElements(functionsArg.values().toArray(), "'functions' cannot have null values.");
		this.functions = new LinkedHashMap<String, Method>(functionsArg);
	}

	public void setTypeLocator(TypeLocator typeLocator) {
		this.typeLocator = typeLocator;
	}


	@Override
	public void afterPropertiesSet() throws Exception {
		if (this.applicationContext != null) {
			this.beanResolver = new BeanFactoryResolver(this.applicationContext);
			ConversionService conversionService = IntegrationUtils.getConversionService(this.applicationContext);
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

			try {
				SpelPropertyAccessorRegistrar propertyAccessorRegistrar = this.applicationContext.getBean(SpelPropertyAccessorRegistrar.class);
				for (Entry<String, PropertyAccessor> entry : propertyAccessorRegistrar.getPropertyAccessors().entrySet()) {
					if (!this.propertyAccessors.containsKey(entry.getKey())) {
						this.propertyAccessors.put(entry.getKey(), entry.getValue());
					}
				}
			}
			catch (NoSuchBeanDefinitionException e) {
				// There is no 'SpelPropertyAccessorRegistrar' bean in the application context.
			}

			ApplicationContext parent = this.applicationContext.getParent();

			if (parent != null && parent.containsBean(IntegrationContextUtils.INTEGRATION_EVALUATION_CONTEXT_BEAN_NAME)) {
				IntegrationEvaluationContextFactoryBean parentFactoryBean =
						parent.getBean("&" + IntegrationContextUtils.INTEGRATION_EVALUATION_CONTEXT_BEAN_NAME,
								IntegrationEvaluationContextFactoryBean.class);

				for (Entry<String, PropertyAccessor> entry : parentFactoryBean.getPropertyAccessors().entrySet()) {
					if (!this.propertyAccessors.containsKey(entry.getKey())) {
						this.propertyAccessors.put(entry.getKey(), entry.getValue());
					}
				}
			}

		}

		sortPropertyAccessors();
		this.initialized = true;
	}

	/**
	 * Sort the property accessors thus: unordered, ordered; when applied to the evaluation
	 * context; unordered will be followed by ordered with a negative order, followed by the default
	 * (currenly reflective) followed by ordered with >= 0 order. Within the same order (or unordered)
	 * the original order is retained.
	 */
	private void sortPropertyAccessors() {
		int orderedStart = 0;
		Iterator<Entry<String, PropertyAccessor>> iterator = this.propertyAccessors.entrySet().iterator();
		Map<String, PropertyAccessor> map = new LinkedHashMap<String, PropertyAccessor>();
		// First "expand" any composite accessors
		while (iterator.hasNext()) {
			Entry<String, PropertyAccessor> entry = iterator.next();
			if (entry.getValue() instanceof CompositePropertyAccessor) {
				Collection<PropertyAccessor> delegates = ((CompositePropertyAccessor) entry.getValue()).accessors();
				int n = 0;
				for (PropertyAccessor accessor : delegates) {
					map.put(entry.getKey() + "." + n++, accessor);
				}
			}
			else {
				map.put(entry.getKey(), entry.getValue());
			}
		}
		// Now build the list in the right sequence.
		List<Entry<String, PropertyAccessor>> list = new LinkedList<Entry<String, PropertyAccessor>>();
		for (Entry<String, PropertyAccessor> entry : map.entrySet()) {
			if (entry.getValue() instanceof Ordered) {
				int i;
				for (i = orderedStart; i < list.size(); i++) {
					if ( ((Ordered) list.get(i).getValue()).getOrder() > ((Ordered) entry.getValue()).getOrder()) {
						list.add(i, entry);
						break;
					}
				}
				if (i >= list.size()) {
					list.add(i, entry);
				}
			}
			else {
				list.add(orderedStart++, entry);
			}
		}
		this.propertyAccessors.clear();
		for (Entry<String, PropertyAccessor> entry : list) {
			this.propertyAccessors.put(entry.getKey(), entry.getValue());
		}
	}

	@Override
	public StandardEvaluationContext getObject() throws Exception {
		StandardEvaluationContext evaluationContext = new StandardEvaluationContext();
		if (this.typeLocator != null) {
			evaluationContext.setTypeLocator(this.typeLocator);
		}

		evaluationContext.setBeanResolver(this.beanResolver);
		evaluationContext.setTypeConverter(this.typeConverter);

		List<PropertyAccessor> propertyAccessors = evaluationContext.getPropertyAccessors();

		boolean addedMapAccessor = false;

		for (PropertyAccessor propertyAccessor : this.propertyAccessors.values()) {
			if (!(propertyAccessor instanceof Ordered) || ((Ordered) propertyAccessor).getOrder() < 0) {
				propertyAccessors.add(propertyAccessors.size() - 1, propertyAccessor);
			}
			else {
				if (!addedMapAccessor) {
					propertyAccessors.add(propertyAccessors.size() - 1, this.mapAccessor);
					addedMapAccessor = true;
				}
				propertyAccessors.add(propertyAccessor);
			}
		}

		// this is not really necessary because we have a direct reference, but that might change.

		evaluationContext.setPropertyAccessors(propertyAccessors);

		if (!addedMapAccessor) {
			evaluationContext.addPropertyAccessor(this.mapAccessor);
		}

		for (Entry<String, Method> functionEntry : this.functions.entrySet()) {
			evaluationContext.registerFunction(functionEntry.getKey(), functionEntry.getValue());
		}

		return evaluationContext;
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
