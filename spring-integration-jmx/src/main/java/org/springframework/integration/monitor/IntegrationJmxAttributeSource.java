/*
 * Copyright © 2016 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2016-present the original author or authors.
 */

package org.springframework.integration.monitor;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.EmbeddedValueResolver;
import org.springframework.core.annotation.MergedAnnotation;
import org.springframework.core.annotation.MergedAnnotations;
import org.springframework.integration.support.management.IntegrationManagedResource;
import org.springframework.jmx.export.annotation.AnnotationJmxAttributeSource;
import org.springframework.jmx.export.metadata.InvalidMetadataException;
import org.springframework.jmx.export.metadata.ManagedResource;
import org.springframework.util.StringValueResolver;

/**
 * The {@link AnnotationJmxAttributeSource} extension to resolve {@link ManagedResource}s
 * via {@link IntegrationManagedResource} on classes.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.3
 */
public class IntegrationJmxAttributeSource extends AnnotationJmxAttributeSource {

	private StringValueResolver valueResolver;

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		super.setBeanFactory(beanFactory);
		if (beanFactory instanceof ConfigurableBeanFactory) {
			this.valueResolver = new EmbeddedValueResolver((ConfigurableBeanFactory) beanFactory);
		}
	}

	@Override
	public ManagedResource getManagedResource(Class<?> beanClass) throws InvalidMetadataException {
		MergedAnnotation<IntegrationManagedResource> ann =
				MergedAnnotations.from(beanClass, MergedAnnotations.SearchStrategy.TYPE_HIERARCHY)
						.get(IntegrationManagedResource.class)
						.withNonMergedAttributes();
		if (!ann.isPresent()) {
			return null;
		}
		Class<?> declaringClass = (Class<?>) ann.getSource();
		Class<?> target = (declaringClass != null && !declaringClass.isInterface() ? declaringClass : beanClass);
		if (!Modifier.isPublic(target.getModifiers())) {
			throw new InvalidMetadataException("@IntegrationManagedResource class '" + target.getName() +
					"' must be public");
		}

		ManagedResource bean = new ManagedResource();
		Map<String, Object> map = ann.asMap();
		List<PropertyValue> list = new ArrayList<>(map.size());
		map.forEach((attrName, attrValue) -> {
			if (!"value".equals(attrName)) {
				Object value = attrValue;
				if (this.valueResolver != null && value instanceof String) {
					value = this.valueResolver.resolveStringValue((String) value);
				}
				list.add(new PropertyValue(attrName, value));
			}
		});
		PropertyAccessorFactory.forBeanPropertyAccess(bean).setPropertyValues(new MutablePropertyValues(list));
		return bean;
	}

}
