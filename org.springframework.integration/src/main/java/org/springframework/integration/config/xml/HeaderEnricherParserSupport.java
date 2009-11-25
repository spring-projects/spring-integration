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

import java.util.HashMap;
import java.util.Map;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.transformer.HeaderEnricher.ExpressionHolder;
import org.springframework.util.ClassUtils;
import org.springframework.util.StringUtils;

/**
 * Base support class for 'header-enricher' parsers.
 * 
 * @author Mark Fisher
 * @since 2.0
 */
public abstract class HeaderEnricherParserSupport extends AbstractTransformerParser {

	private final Map<String, String> elementToNameMap = new HashMap<String, String>();

	private final Map<String, Class<?>> elementToTypeMap = new HashMap<String, Class<?>>();


	@Override
	protected final String getTransformerClassName() {
		return IntegrationNamespaceUtils.BASE_PACKAGE + ".transformer.HeaderEnricher";
	}

	protected boolean shouldOverwrite(Element element) {
		return "true".equals(element.getAttribute("overwrite").toLowerCase());
	}

	protected final void addElementToHeaderMapping(String elementName, String headerName) {
		this.addElementToHeaderMapping(elementName, headerName, null);
	}

	protected final void addElementToHeaderMapping(String elementName, String headerName, Class<?> headerType) {
		this.elementToNameMap.put(elementName, headerName);
		if (headerType != null) {
			this.elementToTypeMap.put(elementName, headerType);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	protected void parseTransformer(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		ManagedMap headers = new ManagedMap();
		this.processHeaders(element, headers, parserContext);
		builder.addConstructorArgValue(headers);
		builder.addPropertyValue("overwrite", this.shouldOverwrite(element)); // TODO: should be a per-header config setting
	}

	protected void processHeaders(Element element, ManagedMap<String, Object> headers, ParserContext parserContext) {
		NodeList childNodes = element.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				String headerName = null;
				Element headerElement = (Element) node;
				String elementName = node.getLocalName();
				Class<?> headerType = null;
				if ("header".equals(elementName)) {
					headerName = headerElement.getAttribute("name");
				}
				else {
					headerName = elementToNameMap.get(elementName);
					headerType = elementToTypeMap.get(elementName);
					if (headerType != null && StringUtils.hasText(headerElement.getAttribute("type"))) {
						parserContext.getReaderContext().error("The " + elementName 
								+ " header does not accept a 'type' attribute. The required type is ["
								+ headerType.getName() + "]", element);
					}
				}
				if (headerType == null) {
					String headerTypeName = headerElement.getAttribute("type");
					if (StringUtils.hasText(headerTypeName)) {
						ClassLoader classLoader = parserContext.getReaderContext().getBeanClassLoader();
						if (classLoader == null) {
							classLoader = getClass().getClassLoader();
						}
						try {
							headerType = ClassUtils.forName(headerTypeName, classLoader);
						}
						catch (Exception e) {
							parserContext.getReaderContext().error("unable to resolve type [" +
									headerTypeName + "] for header '" + headerName + "'", element, e);
						}
					}
				}
				if (headerName != null) {
					String value = headerElement.getAttribute("value");
					String ref = headerElement.getAttribute("ref");
					String expression = headerElement.getAttribute("expression");
					boolean isValue = StringUtils.hasText(value);
					boolean isRef = StringUtils.hasText(ref);
					boolean isExpression = StringUtils.hasText(expression);
					if (!(isValue ^ (isRef ^ isExpression))) {
						parserContext.getReaderContext().error(
								"Exactly one of the 'ref', 'value', or 'expression' attributes is required.", element);
					}
					if (isValue) {
						Object headerValue = (headerType != null) ?
							new TypedStringValue(value, headerType) : value;
						headers.put(headerName, headerValue);
					}
					else if (isExpression) {
						headers.put(headerName, new ExpressionHolder(expression, headerType));
					}
					else {
						headers.put(headerName, new RuntimeBeanReference(ref));
					}
				}
			}
		}
	}

}
