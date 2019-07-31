/*
 * Copyright 2016-2019 the original author or authors.
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
