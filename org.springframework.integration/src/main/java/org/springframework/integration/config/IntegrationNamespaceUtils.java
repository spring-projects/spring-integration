/*
 * Copyright 2002-2008 the original author or authors.
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

import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.endpoint.DefaultEndpointPoller;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * Shared utility methods for integration namespace parsers.
 * 
 * @author Mark Fisher
 * @author Marius Bogoevici
 */
public abstract class IntegrationNamespaceUtils {

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
	public static void setValueIfAttributeDefined(BeanDefinitionBuilder builder, String propertyName,
			Element element, String attributeName) {
		final String attributeValue = element.getAttribute(attributeName);
		if (StringUtils.hasText(attributeValue)) {
			builder.addPropertyValue(propertyName, attributeValue);
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
	public static void setBeanReferenceIfAttributeDefined(BeanDefinitionBuilder builder, String propertyName,
			Element element, String attributeName) {
		final String attributeValue = element.getAttribute(attributeName);
		if (StringUtils.hasText(attributeValue)) {
			builder.addPropertyReference(propertyName, attributeValue);
		}
	}

	public static String parseBeanDefinitionElement(Element element, ParserContext parserContext) {
		BeanDefinitionParserDelegate beanParser =
				new BeanDefinitionParserDelegate(parserContext.getReaderContext());
		beanParser.initDefaults(element.getOwnerDocument().getDocumentElement());
		BeanDefinitionHolder beanDefinitionHolder = beanParser.parseBeanDefinitionElement(element);
		parserContext.registerBeanComponent(new BeanComponentDefinition(beanDefinitionHolder));
		return beanDefinitionHolder.getBeanName();
	}

	/**
	 * Parse a "poller" element and return the bean name of the poller instance.
	 * The name will be generated for a newly created bean, or if the poller
	 * element simply provides a "ref", that value will be returned.
	 * 
	 * @param element the "poller" element to parse
	 * @param parserContext the parserContext for registering a newly created bean definition
	 * @return the name of the poller bean definition
	 */
	public static String parsePoller(Element element, ParserContext parserContext) {
		String ref = element.getAttribute("ref");
		String taskExecutorRef = element.getAttribute("task-executor");
		Element txElement = DomUtils.getChildElementByTagName(element, "transactional");
		if (StringUtils.hasText(ref)) {
			if (StringUtils.hasText(taskExecutorRef) || (txElement != null)) {
				parserContext.getReaderContext().error(
						"Neither the 'task-executor' attribute or 'transactional' sub-element "
						+ "should be provided when using the 'ref' attribute.",
						parserContext.extractSource(element));
			}
			return ref;
		}
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(DefaultEndpointPoller.class);
		if (txElement != null) {
			builder.addPropertyReference("transactionManager", txElement.getAttribute("transaction-manager"));
			builder.addPropertyValue("propagationBehaviorName", txElement.getAttribute("propagation"));
			builder.addPropertyValue("isolationLevelName", txElement.getAttribute("isolation"));
			builder.addPropertyValue("transactionTimeout", txElement.getAttribute("timeout"));
			builder.addPropertyValue("transactionReadOnly", txElement.getAttribute("read-only"));
		}
		if (StringUtils.hasText(taskExecutorRef)) {
			builder.addPropertyReference("taskExecutor", taskExecutorRef);
		}
		String receiveTimeout = element.getAttribute("receive-timeout");
		if (StringUtils.hasText(receiveTimeout)) {
			builder.addPropertyValue("receiveTimeout", Long.parseLong(receiveTimeout));
		}
		String sendTimeout = element.getAttribute("send-timeout");
		if (StringUtils.hasText(sendTimeout)) {
			builder.addPropertyValue("sendTimeout", Long.parseLong(sendTimeout));
		}
		return BeanDefinitionReaderUtils.registerWithGeneratedName(builder.getBeanDefinition(), parserContext.getRegistry());
	}

}
