/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.jpa.support.parametersource;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.NotReadablePropertyException;
import org.springframework.beans.PropertyAccessorFactory;

/**
 *
 * @author Gunnar Hillert
 * @author Gary Russell
 * @author Ngoc Nhan
 *
 * @since 2.2
 *
 */
public class BeanPropertyParameterSource implements ParameterSource {

	private final BeanWrapper beanWrapper;

	private String[] propertyNames;

	/**
	 * Create a new BeanPropertySqlParameterSource for the given bean.
	 * @param object the bean instance to wrap
	 */
	public BeanPropertyParameterSource(Object object) {
		this.beanWrapper = PropertyAccessorFactory.forBeanPropertyAccess(object);
	}

	@Override
	public boolean hasValue(String paramName) {
		return this.beanWrapper.isReadableProperty(paramName);
	}

	@Override
	public Object getValue(String paramName) {
		try {
			return this.beanWrapper.getPropertyValue(paramName);
		}
		catch (NotReadablePropertyException ex) {
			throw new IllegalArgumentException(ex.getMessage()); // NOSONAR - lost stack trace
		}
	}

	/**
	 * Provide access to the property names of the wrapped bean.
	 * Uses support provided in the {@link org.springframework.beans.PropertyAccessor}
	 * interface.
	 * @return an array containing all the known property names
	 */
	public String[] getReadablePropertyNames() {
		if (this.propertyNames == null) {
			final List<String> names = new ArrayList<>();
			PropertyDescriptor[] props = this.beanWrapper.getPropertyDescriptors();
			for (PropertyDescriptor pd : props) {
				if (this.beanWrapper.isReadableProperty(pd.getName())) {
					names.add(pd.getName());
				}
			}
			this.propertyNames = names.toArray(new String[names.size()]);
		}
		return this.propertyNames; // NOSONAR - expose internals
	}

}
