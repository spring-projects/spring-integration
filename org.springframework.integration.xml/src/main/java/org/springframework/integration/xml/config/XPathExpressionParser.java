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
package org.springframework.integration.xml.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.xml.xpath.XPathExpressionFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * 
 * @author Jonas Partner
 *
 */
public class XPathExpressionParser extends AbstractSingleBeanDefinitionParser {
	
	@Override
	protected boolean shouldGenerateId() {
		return false;
	}

	@Override
	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}
	
	
	@SuppressWarnings("unchecked")
	@Override
	protected Class getBeanClass(Element element) {
		return XPathExpressionFactory.class;
	}

	@SuppressWarnings("unchecked")
	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String strXpathExpression = element.getAttribute("expression");
		String strXpathExpressionPrefix = element.getAttribute("ns-prefix");
		String strXpathExpressionNamespace = element.getAttribute("ns-uri");
		String nameSpaceMapRef = element.getAttribute("namespace-map");

		Assert.hasText(strXpathExpression, "xpath-expression attribute is required");

		boolean prefixProvided = StringUtils.hasText(strXpathExpressionPrefix);
		boolean namespaceProvided = StringUtils.hasText(strXpathExpressionNamespace);
		boolean namespaceMapProvided = StringUtils.hasText(nameSpaceMapRef);

		if (prefixProvided || namespaceProvided) {
			Assert.isTrue(prefixProvided && namespaceProvided,
					"Both xpath-prefix and xpath-namespace must be specified if one is specified");
			Assert.isTrue(!namespaceMapProvided, "It is not valid to sepcify both xpath-namespace and namespace-map");
		}

		builder.setFactoryMethod("createXPathExpression");
		builder.addConstructorArgValue(strXpathExpression);

		if (prefixProvided) {
			Map<String, String> namespaceMap = new HashMap<String, String>();
			namespaceMap.put(strXpathExpressionPrefix, strXpathExpressionNamespace);
			builder.addConstructorArgValue(namespaceMap);
		}
		else if (StringUtils.hasText(nameSpaceMapRef)) {
			builder.addConstructorArgReference(nameSpaceMapRef);
		}
		else if (element.getChildNodes().getLength() > 0) {
			NodeList nodeList = element.getChildNodes();
			Element mapElement = null;
			int elementCount = 0;
			for (int i = 0; i < nodeList.getLength(); i++) {
				Node currentNode = nodeList.item(i);
				if (currentNode.getNodeType() == Node.ELEMENT_NODE) {
					mapElement = (Element) currentNode;
					elementCount++;
				}
			}
			Assert.isTrue(elementCount == 1, "Only one namespace map child allowed");
			if (mapElement != null) {
				Map namespaceMap = parseNamespaceMapElement(mapElement, parserContext, builder.getBeanDefinition());
				builder.getBeanDefinition().getConstructorArgumentValues().addGenericArgumentValue(namespaceMap);
			}
		}

	}



	@SuppressWarnings("unchecked")
	protected Map parseNamespaceMapElement(Element element, ParserContext parserContext, BeanDefinition parentDefinition) {
		BeanDefinitionParserDelegate beanParser = new BeanDefinitionParserDelegate(parserContext.getReaderContext());
		beanParser.initDefaults(element.getOwnerDocument().getDocumentElement());
		return beanParser.parseMapElement(element, parentDefinition);
	}

}
