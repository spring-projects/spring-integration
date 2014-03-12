/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.integration.xml.config;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractTransformerParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.xml.transformer.XPathHeaderEnricher;
import org.springframework.integration.xml.transformer.support.XPathExpressionEvaluatingHeaderValueMessageProcessor;
import org.springframework.util.StringUtils;

/**
 * Parser for &lt;xpath-header-enricher&gt; elements.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 * @since 2.0
 */
public class XPathHeaderEnricherParser extends AbstractTransformerParser {

	@Override
	protected final String getTransformerClassName() {
		return XPathHeaderEnricher.class.getName();
	}

	@Override
	protected void parseTransformer(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		ManagedMap<String, Object> headers = new ManagedMap<String, Object>();
		this.processHeaders(element, headers, parserContext);
		builder.addConstructorArgValue(headers);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "default-overwrite");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "should-skip-nulls");
	}

	protected void processHeaders(Element element, ManagedMap<String, Object> headers, ParserContext parserContext) {
		Object source = parserContext.extractSource(element);
		NodeList childNodes = element.getChildNodes();
		for (int i = 0; i < childNodes.getLength(); i++) {
			Node node = childNodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element headerElement = (Element) node;
				String elementName = node.getLocalName();
				if ("header".equals(elementName)) {
					BeanDefinitionBuilder builder =
							BeanDefinitionBuilder.genericBeanDefinition(XPathExpressionEvaluatingHeaderValueMessageProcessor.class);
					String expressionString = headerElement.getAttribute("xpath-expression");
					String expressionRef = headerElement.getAttribute("xpath-expression-ref");
					boolean isExpressionString = StringUtils.hasText(expressionString);
					boolean isExpressionRef = StringUtils.hasText(expressionRef);
					if (!(isExpressionString ^ isExpressionRef)) {
						parserContext.getReaderContext().error(
								"Exactly one of the 'xpath-expression' or 'xpath-expression-ref' attributes is required.", source);
					}
					if (isExpressionString) {
						builder.addConstructorArgValue(expressionString);
					}
					else {
						builder.addConstructorArgReference(expressionRef);
					}
					IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, headerElement, "evaluation-type");
					IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, headerElement, "overwrite");
					IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, headerElement, "header-type");
					String headerName = headerElement.getAttribute("name");
					headers.put(headerName, builder.getBeanDefinition());
				}
			}
		}
	}

}
