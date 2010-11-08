/*
 * Copyright 2002-2010 the original author or authors.
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

import groovy.lang.Binding;
import groovy.lang.GroovyObject;
import groovy.lang.Script;

import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanFactoryAware;
import org.springframework.beans.factory.ListableBeanFactory;
import org.springframework.scripting.groovy.GroovyObjectCustomizer;

/**
 * @author Dave Syer
 * @since 2.0
 */
public class BeanFactoryContextBindingCustomizer implements GroovyObjectCustomizer, BeanFactoryAware {

	private ListableBeanFactory beanFactory;


	public BeanFactoryContextBindingCustomizer() {
		this(null);
	}

	public BeanFactoryContextBindingCustomizer(BeanFactory beanFactory) {
		setBeanFactory(beanFactory);
	}


	public void setBeanFactory(BeanFactory beanFactory) {
		this.beanFactory = (beanFactory instanceof ListableBeanFactory) ? (ListableBeanFactory) beanFactory : null;
	}

	public void customize(GroovyObject goo) {
		if (this.beanFactory != null) {
			Binding binding = ((Script) goo).getBinding();
			for (String name : this.beanFactory.getBeanDefinitionNames()) {
				binding.setVariable(name, this.beanFactory.getBean(name));
			}
		}
	}

}
