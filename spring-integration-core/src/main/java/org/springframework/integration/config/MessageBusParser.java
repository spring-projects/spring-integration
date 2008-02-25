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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.springframework.beans.factory.BeanDefinitionStoreException;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSimpleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.core.Conventions;
import org.springframework.integration.MessagingConfigurationException;
import org.springframework.integration.bus.MessageBus;
import org.springframework.integration.endpoint.ConcurrencyPolicy;
import org.springframework.util.StringUtils;

/**
 * Parser for the <em>message-bus</em> element of the integration namespace.
 * 
 * @author Mark Fisher
 */
public class MessageBusParser extends AbstractSimpleBeanDefinitionParser {

	public static final String MESSAGE_BUS_BEAN_NAME = "internal.MessageBus";

	private static final Class<?> MESSAGE_BUS_CLASS = MessageBus.class;

	private static final String ERROR_CHANNEL_ATTRIBUTE = "error-channel";

	private static final String DEFAULT_CONCURRENCY_ELEMENT = "default-concurrency";

	private static final String DEFAULT_CONCURRENCY_PROPERTY = "defaultConcurrencyPolicy";


	@Override
	protected String resolveId(Element element, AbstractBeanDefinition definition, ParserContext parserContext)
			throws BeanDefinitionStoreException {
		if (parserContext.getRegistry().containsBeanDefinition(MESSAGE_BUS_BEAN_NAME)) {
			throw new MessagingConfigurationException("Only one instance of '" + MESSAGE_BUS_CLASS.getSimpleName()
					+ "' is allowed per ApplicationContext.");
		}
		return MESSAGE_BUS_BEAN_NAME;
	}

	@Override
	protected Class<?> getBeanClass(Element element) {
		return MESSAGE_BUS_CLASS;
	}

	@Override
	protected boolean isEligibleAttribute(String attributeName) {
		return !ERROR_CHANNEL_ATTRIBUTE.equals(attributeName) && super.isEligibleAttribute(attributeName);
	}

	@Override
	protected void postProcess(BeanDefinitionBuilder beanDefinition, Element element) {
		String errorChannelRef = element.getAttribute(ERROR_CHANNEL_ATTRIBUTE);
		if (StringUtils.hasText(errorChannelRef)) {
			beanDefinition.addPropertyReference(Conventions.attributeNameToPropertyName(
					ERROR_CHANNEL_ATTRIBUTE), errorChannelRef);
		}
		this.registerDefaultConcurrencyIfAvailable(beanDefinition, element);
	}

	private void registerDefaultConcurrencyIfAvailable(BeanDefinitionBuilder beanDefinition, Element element) {
		NodeList childNodes = element.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node child = childNodes.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				String localName = child.getLocalName();
				if (DEFAULT_CONCURRENCY_ELEMENT.equals(localName)) {
					ConcurrencyPolicy policy = IntegrationNamespaceUtils.parseConcurrencyPolicy((Element) child);
					beanDefinition.addPropertyValue(DEFAULT_CONCURRENCY_PROPERTY, policy);
				}
			}
		}
	}

}
