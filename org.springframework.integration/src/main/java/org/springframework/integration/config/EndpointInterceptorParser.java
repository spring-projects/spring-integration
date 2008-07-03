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

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.xml.NamespaceHandler;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.Assert;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * A helper class for parsing the sub-elements of an endpoint's
 * <em>interceptors</em> element.
 * 
 * @author Mark Fisher
 */
public class EndpointInterceptorParser {

	private final Map<String, BeanDefinitionRegisteringParser> parsers = new HashMap<String, BeanDefinitionRegisteringParser>();

	public EndpointInterceptorParser() {
		this.parsers.put("transaction-interceptor", new TransactionInterceptorParser());
		this.parsers.put("concurrency-interceptor", new ConcurrencyInterceptorParser());
	}

	@SuppressWarnings("unchecked")
	public ManagedList parseEndpointInterceptors(Element element, ParserContext parserContext) {
		ManagedList interceptors = new ManagedList();
		NodeList childNodes = element.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node child = childNodes.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				Element childElement = (Element) child;
				String localName = child.getLocalName();
				if ("bean".equals(localName)) {
					BeanDefinitionParserDelegate beanParser = new BeanDefinitionParserDelegate(parserContext
							.getReaderContext());
					beanParser.initDefaults(childElement.getOwnerDocument().getDocumentElement());
					BeanDefinitionHolder beanDefinitionHolder = beanParser.parseBeanDefinitionElement(childElement);
					parserContext.registerBeanComponent(new BeanComponentDefinition(beanDefinitionHolder));
					interceptors.add(new RuntimeBeanReference(beanDefinitionHolder.getBeanName()));
				}
				else if ("ref".equals(localName)) {
					String ref = childElement.getAttribute("bean");
					interceptors.add(new RuntimeBeanReference(ref));
				}
				else {
					BeanDefinitionRegisteringParser parser = this.parsers.get(localName);
					String interceptorBeanName;
					if (parser == null) {
						interceptorBeanName = handleNonstandardInterceptor(childElement, parserContext);
					}
					else {
						interceptorBeanName = parser.parse(childElement, parserContext);
					}
					interceptors.add(new RuntimeBeanReference(interceptorBeanName));
				}
			}
		}
		return interceptors;
	}

	protected String handleNonstandardInterceptor(Element childElement, ParserContext parserContext) {
		NamespaceHandler handlerFromOtherNamespace = parserContext.getReaderContext().getNamespaceHandlerResolver()
				.resolve(childElement.getNamespaceURI());
		AbstractBeanDefinition interceptorDefintiion = ((AbstractBeanDefinition) handlerFromOtherNamespace.parse(
				childElement, parserContext));
		String beanName = (String) interceptorDefintiion.getMetadataAttribute("interceptorName").getValue();
		Assert.hasText("No value for interceptorName provided by namespace handler for element "
				+ childElement.getNodeName());
		return beanName;
	}

}
