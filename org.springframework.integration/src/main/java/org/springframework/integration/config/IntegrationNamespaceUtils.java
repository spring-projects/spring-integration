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
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.core.Conventions;
import org.springframework.integration.ConfigurationException;
import org.springframework.integration.scheduling.CronTrigger;
import org.springframework.integration.scheduling.IntervalTrigger;
import org.springframework.integration.scheduling.Trigger;
import org.springframework.transaction.support.DefaultTransactionDefinition;
import org.springframework.util.StringUtils;

/**
 * Shared utility methods for integration namespace parsers.
 * 
 * @author Mark Fisher
 * @author Marius Bogoevici
 */
public abstract class IntegrationNamespaceUtils {

	/**
	 * Populates the specified bean definition property with the value
	 * of the attribute whose name is provided if that attribute is
	 * defined in the given element.
	 * 
	 * @param beanDefinition the bean definition to be configured
	 * @param element the XML element where the attribute should be defined
	 * @param attributeName the name of the attribute whose value will be
	 * used to populate the property
	 * @param propertyName the name of the property to be populated
	 */
	public static void setValueIfAttributeDefined(BeanDefinitionBuilder builder,
			Element element, String attributeName, String propertyName) {
		String attributeValue = element.getAttribute(attributeName);
		if (StringUtils.hasText(attributeValue)) {
			builder.addPropertyValue(propertyName, attributeValue);
		}
	}

	/**
	 * Populates the bean definition property corresponding to the specified
	 * attributeName with the value of that attribute if it is defined in the
	 * given element.
	 * 
	 * <p>The property name will be the camel-case equivalent of the lower
	 * case hyphen separated attribute (e.g. the "foo-bar" attribute would
	 * match the "fooBar" property).
	 * 
	 * @see Conventions#attributeNameToPropertyName(String)
	 * 
	 * @param beanDefinition - the bean definition to be configured
	 * @param element - the XML element where the attribute should be defined
	 * @param attributeName - the name of the attribute whose value will be set
	 * on the property
	 */
	public static void setValueIfAttributeDefined(BeanDefinitionBuilder builder,
			Element element, String attributeName) {
		setValueIfAttributeDefined(builder, element, attributeName,
				Conventions.attributeNameToPropertyName(attributeName));
	}

	/**
	 * Populates the specified bean definition property with the reference
	 * to a bean. The bean reference is identified by the value from the
	 * attribute whose name is provided if that attribute is defined in
	 * the given element.
	 * 
	 * @param beanDefinition the bean definition to be configured
	 * @param element the XML element where the attribute should be defined
	 * @param attributeName the name of the attribute whose value will be
	 * used as a bean reference to populate the property
	 * @param propertyName the name of the property to be populated
	 */
	public static void setReferenceIfAttributeDefined(BeanDefinitionBuilder builder,
			Element element, String attributeName, String propertyName) {
		String attributeValue = element.getAttribute(attributeName);
		if (StringUtils.hasText(attributeValue)) {
			builder.addPropertyReference(propertyName, attributeValue);
		}
	}

	/**
	 * Populates the bean definition property corresponding to the specified
	 * attributeName with the reference to a bean identified by the value of
	 * that attribute if the attribute is defined in the given element.
	 * 
	 * <p>The property name will be the camel-case equivalent of the lower
	 * case hyphen separated attribute (e.g. the "foo-bar" attribute would
	 * match the "fooBar" property).
	 * 
	 * @see Conventions#attributeNameToPropertyName(String)
	 * 
	 * @param beanDefinition - the bean definition to be configured
	 * @param element - the XML element where the attribute should be defined
	 * @param attributeName - the name of the attribute whose value will be
	 * used as a bean reference to populate the property
	 * 
	 * @see Conventions#attributeNameToPropertyName(String)
	 */
	public static void setReferenceIfAttributeDefined(BeanDefinitionBuilder builder,
			Element element, String attributeName) {
		setReferenceIfAttributeDefined(builder, element, attributeName,
				Conventions.attributeNameToPropertyName(attributeName));
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
	 * Parse a "poller" element to create a Trigger and add it to the property values of the target builder.
	 * 
	 * @param pollerElement the "poller" element to parse
	 * @param targetBuilder the builder that expects the "trigger" property
	 */
	public static void configureTrigger(Element pollerElement, BeanDefinitionBuilder targetBuilder) {
		Trigger trigger = null;
		String interval = pollerElement.getAttribute("period");
		String cron = pollerElement.getAttribute("cron");
		if (!(StringUtils.hasText(interval) ^ StringUtils.hasText(cron))) {
			throw new ConfigurationException(
					"A <poller> element must define either a period or a cron expression (but not both).");
		}
		if (StringUtils.hasText(interval)) {
			Long period = Long.valueOf(interval);
			IntervalTrigger intervalTrigger = new IntervalTrigger(period);
			String initialDelay = pollerElement.getAttribute("initial-delay");
			if (StringUtils.hasText(initialDelay)) {
				intervalTrigger.setInitialDelay(Long.valueOf(initialDelay));
			}
			intervalTrigger.setFixedRate("true".equals(pollerElement.getAttribute("fixed-rate").toLowerCase()));
			trigger = intervalTrigger;
		}
		if (StringUtils.hasText(pollerElement.getAttribute("cron"))) {
		   trigger = new CronTrigger(pollerElement.getAttribute("cron"));
		}
		targetBuilder.addPropertyValue("trigger", trigger);
	}

	/**
	 * Parse a "transactional" element and configure the "transactionManager" and "transactionDefinition"
	 * properties for the target builder.
	 * 
	 * @param txElement the "transactional" element to parse
	 * @param targetBuilder the builder that expects the "transactionManager" and "transactionDefinition" properties
	 */
	public static void configureTransactionAttributes(Element txElement, BeanDefinitionBuilder targetBuilder) {
		targetBuilder.addPropertyReference("transactionManager", txElement.getAttribute("transaction-manager"));
		DefaultTransactionDefinition txDefinition = new DefaultTransactionDefinition();
		txDefinition.setPropagationBehaviorName(
				DefaultTransactionDefinition.PREFIX_PROPAGATION + txElement.getAttribute("propagation"));
		txDefinition.setIsolationLevelName(
				DefaultTransactionDefinition.PREFIX_ISOLATION + txElement.getAttribute("isolation"));
		txDefinition.setTimeout(Integer.valueOf(txElement.getAttribute("timeout")));
		txDefinition.setReadOnly(txElement.getAttribute("read-only").equalsIgnoreCase("true"));
		targetBuilder.addPropertyValue("transactionDefinition", txDefinition);
	}

}
