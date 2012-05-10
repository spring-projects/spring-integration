/*
 * Copyright 2002-2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.springframework.integration.groovy;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.util.Assert;

/**
 * {@link BeanFactory} decorator implementation which applies {@link BeanFilter} instance on <code>original</code>
 * {@link ConfigurableListableBeanFactory} for getting Spring bean at runtime. Can be used as 'bean resolving strategy'
 * when it is necessary to ban some beans for execution context.
 *
 * @author Artem Bilan
 * @since 2.2
 */
public class FilteredBeanFactoryDecorator implements BeanFactory, BeanFactoryAware {

	private ConfigurableListableBeanFactory original;

	private BeanFilter beanFilter;

	/**
	 * Create a new instance of the {@link FilteredBeanFactoryDecorator} class.
	 * <p>The BeanFactory to access must be set via <code>setBeanFactory</code>.
	 * This will happen automatically if this 'decorator' is defined within an
	 * ApplicationContext thereby receiving the callback upon initialization.
	 *
	 * @see #setBeanFactory
	 */
	public FilteredBeanFactoryDecorator() {
	}

	/**
	 * Create a new instance of the {@link FilteredBeanFactoryDecorator} class.
	 * <p>Use of this constructor is redundant if this object is being created
	 * by a Spring IoC container as the supplied {@link BeanFactory} will be
	 * replaced by the {@link BeanFactory} that creates it (c.f. the
	 * {@link BeanFactoryAware} contract). So only use this constructor if you
	 * are instantiating this object explicitly rather than defining a bean.
	 *
	 * @see #setBeanFactory
	 * @see #setBeanFilter
	 */
	public FilteredBeanFactoryDecorator(BeanFactory beanFactory, BeanFilter beanFilter) {
		assertBeanFactory(beanFactory);
		this.original = (ConfigurableListableBeanFactory) beanFactory;

		Assert.notNull(beanFilter, "beanFilter must not be null");
		this.beanFilter = beanFilter;
	}

	/**
	 * Setup this decorator with some {@link BeanFilter}. Will be used on 'original' {@link BeanFactory}
	 * to apply some filtering logic on getting Spring beans.
	 *
	 * @param beanFilter - an instance of {@link BeanFilter}, required.
	 */
	public void setBeanFilter(BeanFilter beanFilter) {
		Assert.notNull(beanFilter, "beanFilter must not be null");
		this.beanFilter = beanFilter;
	}

	public void setBeanFactory(BeanFactory beanFactory) throws BeansException {
		assertBeanFactory(beanFactory);
		this.original = (ConfigurableListableBeanFactory) beanFactory;
	}

	public Object getBean(String name) throws BeansException {
		return this.beanFilter.getBeanIfMatch(this.original, name);
	}

	public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
		return this.beanFilter.getBeanIfMatch(this.original, name, requiredType);
	}

	public <T> T getBean(Class<T> requiredType) throws BeansException {
		return this.beanFilter.getBeanIfMatch(this.original, requiredType);
	}

	public Object getBean(String name, Object... args) throws BeansException {
		return this.beanFilter.getBeanIfMatch(this.original, name, args);
	}

	public boolean containsBean(String name) {
		return this.original.containsBean(name);
	}

	public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
		return this.original.isSingleton(name);
	}

	public boolean isPrototype(String name) throws NoSuchBeanDefinitionException {
		return this.original.isPrototype(name);
	}

	public boolean isTypeMatch(String name, Class targetType) throws NoSuchBeanDefinitionException {
		return this.original.isTypeMatch(name, targetType);
	}

	public Class<?> getType(String name) throws NoSuchBeanDefinitionException {
		return this.original.getType(name);
	}

	public String[] getAliases(String name) {
		return this.original.getAliases(name);
	}

	private static void assertBeanFactory(BeanFactory beanFactory) {
		Assert.notNull(beanFactory, "beanFactory must not be null");
		Assert.isInstanceOf(ConfigurableListableBeanFactory.class, beanFactory,
				"beanFactory must be an instance of org.springframework.beans.factory.config.ConfigurableListableBeanFactory");
	}

}
