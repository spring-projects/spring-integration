/*
 * Copyright 2002-2007 the original author or authors.
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

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.integration.endpoint.ConcurrencyPolicy;
import org.springframework.util.StringUtils;

/**
 * Shared utility methods for integration namespace parsers.
 * 
 * @author Mark Fisher
 * @author Marius Bogoevici
 */
public abstract class IntegrationNamespaceUtils {

	private static final String CORE_SIZE_ATTRIBUTE = "core";

	private static final String MAX_SIZE_ATTRIBUTE = "max";

	private static final String QUEUE_CAPACITY_ATTRIBUTE = "queue-capacity";

	private static final String KEEP_ALIVE_ATTRIBUTE = "keep-alive";

	public static ConcurrencyPolicy parseConcurrencyPolicy(Element element) {
		ConcurrencyPolicy policy = new ConcurrencyPolicy();
		String coreSize = element.getAttribute(CORE_SIZE_ATTRIBUTE);
		String maxSize = element.getAttribute(MAX_SIZE_ATTRIBUTE);
		String queueCapacity = element.getAttribute(QUEUE_CAPACITY_ATTRIBUTE);
		String keepAlive = element.getAttribute(KEEP_ALIVE_ATTRIBUTE);
		if (StringUtils.hasText(coreSize)) {
			policy.setCoreSize(Integer.parseInt(coreSize));
		}
		if (StringUtils.hasText(maxSize)) {
			policy.setMaxSize(Integer.parseInt(maxSize));
		}
		if (StringUtils.hasText(queueCapacity)) {
			policy.setQueueCapacity(Integer.parseInt(queueCapacity));
		}
		if (StringUtils.hasText(keepAlive)) {
			policy.setKeepAliveSeconds(Integer.parseInt(keepAlive));
		}
		return policy;
	}

	/**
	 * Populates the property identified by propertyName on the bean definition
	 * to the value of the attribute specified by attributeName, if that
	 * attribute is defined in the element
	 * 
	 * @param beanDefinition - the bean definition to be configured
	 * @param propertyName - the name of the bean property to be set
	 * @param element - the XML element where the attribute should be defined
	 * @param attributeName - the name of the attribute whose value will be set
	 * on the property
	 */
	public static void setValueIfAttributeDefined(RootBeanDefinition beanDefinition, String propertyName,
			Element element, String attributeName) {
		final String attributeValue = element.getAttribute(attributeName);
		if (StringUtils.hasText(attributeValue)) {
			beanDefinition.getPropertyValues().addPropertyValue(propertyName, attributeValue);
		}
	}

	/**
	 * Populates the property given by propertyName on the given bean definition
	 * to a reference to a bean identified by the value of the attribute
	 * specified by attributeName, if that attribute is defined in the element
	 * 
	 * @param beanDefinition - the bean definition to be configured
	 * @param propertyName - the name of the bean property to be set
	 * @param element - the XML element where the attribute should be defined
	 * @param attributeName - the id of the bean which will be used to populate
	 * the property
	 */
	public static void setBeanReferenceIfAttributeDefined(RootBeanDefinition beanDefinition, String propertyName,
			Element element, String attributeName) {
		final String attributeValue = element.getAttribute(attributeName);
		if (StringUtils.hasText(attributeValue)) {
			beanDefinition.getPropertyValues().addPropertyValue(propertyName, new RuntimeBeanReference(attributeValue));
		}
	}

}
