/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.xml.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.BeanDefinitionParserDelegate;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.springframework.xml.xpath.XPathExpressionFactory;

/**
 * Parser for the &lt;xpath-expression&gt; element.
 *
 * @author Jonas Partner
 * @author Soby Chacko
 * @author Artem Bilan
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

	@Override
	protected Class<?> getBeanClass(Element element) {
		return XPathExpressionFactory.class;
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String expression = element.getAttribute("expression");
		Assert.hasText(expression, "The 'expression' attribute is required.");

		builder.setFactoryMethod("createXPathExpression");
		builder.addConstructorArgValue(expression);

		parseAndPopulateNamespaceMap(element, parserContext, builder);
	}

	static void parseAndPopulateNamespaceMap(Element element, ParserContext parserContext,
			BeanDefinitionBuilder builder) {
		String nsPrefix = element.getAttribute("ns-prefix");
		String nsUri = element.getAttribute("ns-uri");
		String namespaceMapRef = element.getAttribute("namespace-map");

		List<Element> mapElements = DomUtils.getChildElementsByTagName(element, "map");

		boolean prefixProvided = StringUtils.hasText(nsPrefix);
		boolean namespaceProvided = StringUtils.hasText(nsUri);
		boolean namespaceMapProvided = StringUtils.hasText(namespaceMapRef);

		boolean mapSubElementProvided = !mapElements.isEmpty();

		if (prefixProvided || namespaceProvided) {
			Assert.isTrue(prefixProvided && namespaceProvided,
					"Both 'ns-prefix' and 'ns-uri' must be specified if one is specified.");
			Assert.isTrue(!namespaceMapProvided, "It is not valid to specify both, " +
					"the namespace attributes ('ns-prefix' and 'ns-uri') and the 'namespace-map' attribute.");
			Assert.isTrue(!mapSubElementProvided, "It is not valid to specify both, " +
					"the namespace attributes ('ns-prefix' and 'ns-uri') and the 'map' sub-element.");
		}
		else if (mapSubElementProvided) {
			Assert.isTrue(!namespaceMapProvided, "It is not valid to specify both, " +
					"the 'namespace-map' attribute and the 'map' sub-element.");
		}

		if (prefixProvided) {
			Map<String, String> namespaceMap = new HashMap<String, String>(1);
			namespaceMap.put(nsPrefix, nsUri);
			builder.addConstructorArgValue(namespaceMap);
		}
		else if (namespaceMapProvided) {
			builder.addConstructorArgReference(namespaceMapRef);
		}
		else if (mapSubElementProvided) {
			Element mapElement = mapElements.get(0);
			if (mapElement != null) {
				BeanDefinitionParserDelegate beanParser = parserContext.getDelegate();
				beanParser.initDefaults(mapElement.getOwnerDocument().getDocumentElement(), beanParser);
				builder.addConstructorArgValue(beanParser.parseMapElement(mapElement, builder.getRawBeanDefinition()));
			}
		}
	}

}
