/*
 * Copyright © 2013 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2013-present the original author or authors.
 */

package org.springframework.integration.config.xml;

import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.expression.SpelPropertyAccessorRegistrar;
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;spel-property-accessors&gt; element.
 *
 * @author Artem Bilan
 * @author Gary Russell
 * @author Christian Tzolov
 *
 * @since 3.0
 */
public class SpelPropertyAccessorsParser implements BeanDefinitionParser {

	private final Lock lock = new ReentrantLock();

	private final Map<String, Object> propertyAccessors = new ManagedMap<String, Object>();

	@Override
	public BeanDefinition parse(Element element, ParserContext parserContext) {
		initializeSpelPropertyAccessorRegistrarIfNecessary(parserContext);

		BeanDefinitionParserDelegate delegate = parserContext.getDelegate();

		NodeList children = element.getChildNodes();

		for (int i = 0; i < children.getLength(); i++) {
			Node node = children.item(i);
			String propertyAccessorName;
			Object propertyAccessor;
			if (node instanceof Element &&
					!delegate.nodeNameEquals(node, BeanDefinitionParserDelegate.DESCRIPTION_ELEMENT)) {
				Element ele = (Element) node;

				if (delegate.nodeNameEquals(ele, BeanDefinitionParserDelegate.BEAN_ELEMENT)) {
					propertyAccessorName = ele.getAttribute(BeanDefinitionParserDelegate.ID_ATTRIBUTE);
					if (!StringUtils.hasText(propertyAccessorName)) {
						parserContext.getReaderContext()
								.error("The '<bean>' 'id' attribute is required within 'spel-property-accessors'.", ele);
						return null;
					}
					propertyAccessor = delegate.parseBeanDefinitionElement(ele);
				}
				else if (delegate.nodeNameEquals(ele, BeanDefinitionParserDelegate.REF_ELEMENT)) {
					BeanReference propertyAccessorRef = (BeanReference) delegate.parsePropertySubElement(ele, null);
					propertyAccessorName = propertyAccessorRef.getBeanName(); // NOSONAR not null
					propertyAccessor = propertyAccessorRef;
				}
				else {
					parserContext.getReaderContext().error("Only '<bean>' and '<ref>' elements are allowed.", element);
					return null;
				}

				this.propertyAccessors.put(propertyAccessorName, propertyAccessor);
			}
		}

		return null;
	}

	private void initializeSpelPropertyAccessorRegistrarIfNecessary(ParserContext parserContext) {
		this.lock.lock();
		try {
			if (!parserContext.getRegistry()
					.containsBeanDefinition(IntegrationContextUtils.SPEL_PROPERTY_ACCESSOR_REGISTRAR_BEAN_NAME)) {

				BeanDefinitionBuilder registrarBuilder = BeanDefinitionBuilder
						.genericBeanDefinition(SpelPropertyAccessorRegistrar.class)
						.setRole(BeanDefinition.ROLE_INFRASTRUCTURE)
						.addConstructorArgValue(this.propertyAccessors);
				parserContext.getRegistry()
						.registerBeanDefinition(IntegrationContextUtils.SPEL_PROPERTY_ACCESSOR_REGISTRAR_BEAN_NAME,
								registrarBuilder.getBeanDefinition());
			}
		}
		finally {
			this.lock.unlock();
		}
	}

}
