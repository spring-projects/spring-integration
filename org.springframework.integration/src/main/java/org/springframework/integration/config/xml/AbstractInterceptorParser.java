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

package org.springframework.integration.config.xml;

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.NamespaceHandler;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.Assert;

/**
 * A helper class for parsing the sub-elements of an endpoint
 * or channel-adapter's <em>interceptors</em> element.
 * 
 * @author Mark Fisher
 */
public abstract class AbstractInterceptorParser {

	private final Map<String, BeanDefinitionRegisteringParser> parsers = new HashMap<String, BeanDefinitionRegisteringParser>();

	private volatile boolean initialized;

	private final Object initializationMonitor = new Object();


	protected abstract Map<String, BeanDefinitionRegisteringParser> getParserMap();

	private void initializeParserMap() {
		synchronized (this.initializationMonitor) {
			if (!this.initialized) {
				Map<String, BeanDefinitionRegisteringParser> parserMap = this.getParserMap();
				if (parserMap != null) {
					this.parsers.putAll(parserMap);
				}
				this.initialized = true;
			}
		}
	}

	@SuppressWarnings("unchecked")
	public ManagedList parseInterceptors(Element element, ParserContext parserContext) {
		if (!initialized) {
			this.initializeParserMap();
		}
		ManagedList interceptors = new ManagedList();
		NodeList childNodes = element.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node child = childNodes.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				Element childElement = (Element) child;
				String localName = child.getLocalName();
				if ("bean".equals(localName)) {
					interceptors.add(new RuntimeBeanReference(
							IntegrationNamespaceUtils.parseBeanDefinitionElement(childElement, parserContext)));
				}
				else if ("ref".equals(localName)) {
					String ref = childElement.getAttribute("bean");
					interceptors.add(new RuntimeBeanReference(ref));
				}
				else {
					BeanDefinitionRegisteringParser parser = this.parsers.get(localName);
					String interceptorBeanName = null;
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
		NamespaceHandler handler = parserContext.getReaderContext().getNamespaceHandlerResolver()
				.resolve(childElement.getNamespaceURI());
		AbstractBeanDefinition interceptorDefinition =
				((AbstractBeanDefinition) handler.parse(childElement, parserContext));
		String beanName = (String) interceptorDefinition.getMetadataAttribute("interceptorName").getValue();
		Assert.hasText("No value for interceptorName provided by namespace handler for element '"
				+ childElement.getNodeName() + "'");
		return beanName;
	}

}
