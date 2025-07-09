/*
 * Copyright 2002-present the original author or authors.
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

package org.springframework.integration.config.xml;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.parsing.BeanComponentDefinition;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.xml.ParserContext;

/**
 * A helper class for parsing the sub-elements of a channel's
 * <em>interceptors</em> element.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Ngoc Nhan
 */
public class ChannelInterceptorParser {

	private final Map<String, BeanDefinitionRegisteringParser> parsers;

	public ChannelInterceptorParser() {
		this.parsers = new HashMap<>();
		this.parsers.put("wire-tap", new WireTapParser());
	}

	@SuppressWarnings({"unchecked", "rawtypes"})
	public ManagedList parseInterceptors(Element element, ParserContext parserContext) {
		ManagedList interceptors = new ManagedList();
		NodeList childNodes = element.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node child = childNodes.item(i);
			if (child.getNodeType() == Node.ELEMENT_NODE) {
				Element childElement = (Element) child;
				String localName = child.getLocalName();
				if ("bean".equals(localName)) {
					BeanDefinitionParserDelegate delegate = parserContext.getDelegate();
					BeanDefinitionHolder holder = delegate.parseBeanDefinitionElement(childElement);
					holder = delegate.decorateBeanDefinitionIfRequired(childElement, Objects.requireNonNull(holder));
					parserContext.registerBeanComponent(new BeanComponentDefinition(holder));
					interceptors.add(new RuntimeBeanReference(holder.getBeanName()));
				}
				else if ("ref".equals(localName)) {
					String ref = childElement.getAttribute("bean");
					interceptors.add(new RuntimeBeanReference(ref));
				}
				else {
					BeanDefinitionRegisteringParser parser = this.parsers.get(localName);
					if (parser == null) {
						parserContext.getReaderContext().error(
								"unsupported interceptor element '" + localName + "'", childElement);
						// Redundant Exception is here to satisfy NullAway warning parser.parse statement below.
						throw new IllegalStateException("unsupported interceptor element '" + localName + "'");
					}
					String interceptorBeanName = parser.parse(childElement, parserContext);
					interceptors.add(new RuntimeBeanReference(interceptorBeanName));
				}
			}
		}
		return interceptors;
	}

}
