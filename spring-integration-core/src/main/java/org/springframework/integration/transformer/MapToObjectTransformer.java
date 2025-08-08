/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.transformer;

import java.util.Map;

import org.springframework.beans.BeanUtils;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.validation.DataBinder;

/**
 * Will transform Map to an instance of Object. There are two ways to specify the type of the transformed Object.
 * You can use one of two constructors. The constructor that takes the Class&lt;?&gt; as an argument will construct the Object of
 * that type. There is another constructor that takes a 'beanName' as an argument and will populate this bean with transformed data.
 * Such bean must be of 'prototype' scope otherwise {@link MessageTransformationException} will be thrown.
 * This transformer is integrated with the {@link ConversionService} allowing values in the Map to be converted
 * to types that represent the properties of the Object.
 *
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @since 2.0
 */
public class MapToObjectTransformer extends AbstractPayloadTransformer<Map<?, ?>, Object> {

	private final Class<?> targetClass;

	private final String targetBeanName;

	/**
	 * @param targetClass The target class.
	 */
	public MapToObjectTransformer(Class<?> targetClass) {
		Assert.notNull(targetClass, "targetClass must not be null");
		this.targetClass = targetClass;
		this.targetBeanName = null;
	}

	/**
	 * @param beanName The bean name.
	 */
	public MapToObjectTransformer(String beanName) {
		Assert.hasText(beanName, "beanName must not be empty");
		this.targetBeanName = beanName;
		this.targetClass = null;
	}

	@Override
	public String getComponentType() {
		return "map-to-object-transformer";
	}

	@Override
	protected void onInit() {
		if (StringUtils.hasText(this.targetBeanName)) {
			Assert.isTrue(this.getBeanFactory().isPrototype(this.targetBeanName),
					"target bean [" + this.targetBeanName + "] must have 'prototype' scope");
		}
	}

	@Override
	protected Object transformPayload(Map<?, ?> payload) {
		Object target = (this.targetClass != null)
				? BeanUtils.instantiateClass(this.targetClass)
				: this.getBeanFactory().getBean(this.targetBeanName);

		DataBinder binder = new DataBinder(target);
		ConversionService conversionService = getConversionService();
		if (conversionService == null) {
			conversionService = DefaultConversionService.getSharedInstance();
		}
		binder.setConversionService(conversionService);
		binder.bind(new MutablePropertyValues(payload));

		return target;
	}

}
