/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
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
			final List<String> names = new ArrayList<String>();
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
