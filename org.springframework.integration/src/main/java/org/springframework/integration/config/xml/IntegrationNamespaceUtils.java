/*
 * Copyright 2002-2009 the original author or authors.
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

package org.springframework.integration.config.xml;

import java.util.List;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.core.Conventions;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * Shared utility methods for integration namespace parsers.
 * 
 * @author Mark Fisher
 * @author Marius Bogoevici
 * @author Alex Peters
 */
public abstract class IntegrationNamespaceUtils {

	static final String BASE_PACKAGE = "org.springframework.integration";
	static final String REF_ATTRIBUTE = "ref";
	static final String METHOD_ATTRIBUTE = "method";
	static final String ORDER = "order";
	


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

	/**
	 * Provides a user friendly description of an element based on its node
	 * name and, if available, its "id" attribute value. This is useful for
	 * creating error messages from within bean definition parsers.
	 */
	public static String createElementDescription(Element element) {
		String elementId = "'" + element.getNodeName() + "'";
		String id = element.getAttribute("id");
		if (StringUtils.hasText(id)) {
			elementId += " with id='" + id + "'";
		}
		return elementId;
	}

	/**
	 * Parse a "poller" element to provide a reference for the target
	 * BeanDefinitionBuilder. If the poller element does not contain a "ref"
	 * attribute, this will create and register a PollerMetadata instance and
	 * then add it as a property reference of the target builder.
	 * 
	 * @param pollerElement the "poller" element to parse
	 * @param targetBuilder the builder that expects the "trigger" property
	 * @param parserContext the parserContext for the target builder
	 */
	public static void configurePollerMetadata(Element pollerElement, BeanDefinitionBuilder targetBuilder, ParserContext parserContext) {
		String pollerMetadataRef = null;
		if (pollerElement.hasAttribute("ref")) {
			if (pollerElement.getAttributes().getLength() != 1) {
				parserContext.getReaderContext().error(
						"A 'poller' element that provides a 'ref' must have no other attributes.", pollerElement);
			}
			if (pollerElement.getChildNodes().getLength() != 0) {
				parserContext.getReaderContext().error(
						"A 'poller' element that provides a 'ref' must have no child elements.", pollerElement);
			}
			pollerMetadataRef = pollerElement.getAttribute("ref");
		}
		else {
			BeanDefinition beanDefinition = parserContext.getDelegate().parseCustomElement(
					pollerElement, targetBuilder.getBeanDefinition());
			if (beanDefinition == null) {
				parserContext.getReaderContext().error("BeanDefinition must not be null", pollerElement);
			}
			pollerMetadataRef = BeanDefinitionReaderUtils.generateBeanName(beanDefinition, parserContext.getRegistry());
			BeanDefinitionHolder holder = new BeanDefinitionHolder(beanDefinition, pollerMetadataRef);
			BeanDefinitionReaderUtils.registerBeanDefinition(holder, parserContext.getRegistry());
		}
		targetBuilder.addPropertyReference("pollerMetadata", pollerMetadataRef);
	}

	public static BeanDefinition parseInnerHandlerDefinition(Element element, ParserContext parserContext){
		// parses out inner bean definition for concrete implementation if defined
		List<Element> childElements = DomUtils.getChildElementsByTagName(element, "bean");
		BeanDefinition innerDefinition = null;
		if (childElements != null && childElements.size() == 1){
			Element beanElement = childElements.get(0);
			BeanDefinitionParserDelegate delegate = parserContext.getDelegate();
			innerDefinition = delegate.parseBeanDefinitionElement(beanElement).getBeanDefinition();
		}
		
		String ref = element.getAttribute(REF_ATTRIBUTE);
		Assert.isTrue(!(StringUtils.hasText(ref) && innerDefinition != null), "Ambiguous definition. Inner bean " + 
				(innerDefinition == null ? innerDefinition : innerDefinition.getBeanClassName()) + " declaration and \"ref\" " + ref + 
		       " are not allowed together.");
		return innerDefinition;
	}

}
