/*
 * Copyright 2016 the original author or authors.
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

package org.springframework.integration.monitor;

import org.springframework.beans.annotation.AnnotationBeanUtils;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.config.EmbeddedValueResolver;
import org.springframework.core.annotation.AnnotationUtils;
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
 * @since 4.3
 */
public class IntegrationJmxAttributeSource extends AnnotationJmxAttributeSource {

	private StringValueResolver valueResolver;

	public void setValueResolver(StringValueResolver valueResolver) {
		this.valueResolver = valueResolver;
	}

	@Override
	public void setBeanFactory(BeanFactory beanFactory) {
		super.setBeanFactory(beanFactory);
		if (beanFactory instanceof ConfigurableBeanFactory) {
			this.valueResolver = new EmbeddedValueResolver((ConfigurableBeanFactory) beanFactory);
		}
	}

	@Override
	public ManagedResource getManagedResource(Class<?> beanClass) throws InvalidMetadataException {
		IntegrationManagedResource ann = AnnotationUtils.getAnnotation(beanClass, IntegrationManagedResource.class);
		if (ann == null) {
			return null;
		}
		ManagedResource managedResource = new ManagedResource();
		AnnotationBeanUtils.copyPropertiesToBean(ann, managedResource, this.valueResolver);
		return managedResource;
	}

}
