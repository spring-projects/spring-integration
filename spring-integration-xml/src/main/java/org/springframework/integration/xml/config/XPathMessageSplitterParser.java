/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.xml.config;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractConsumerEndpointParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.xml.splitter.XPathMessageSplitter;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * @author Jonas Partner
 * @author Artem Bilan
 * @author Gary Russell
 */
public class XPathMessageSplitterParser extends AbstractConsumerEndpointParser {

	@Override
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(XPathMessageSplitter.class);
		String xPathExpressionRef = element.getAttribute("xpath-expression-ref");
		NodeList xPathExpressionNodes = element.getElementsByTagNameNS(element.getNamespaceURI(), "xpath-expression");
		Assert.isTrue(xPathExpressionNodes.getLength() <= 1, "At most one xpath-expression child may be specified.");
		boolean hasChild = xPathExpressionNodes.getLength() == 1;
		boolean hasReference = StringUtils.hasText(xPathExpressionRef);
		Assert.isTrue(hasChild ^ hasReference,
				"Exactly one of 'xpath-expression' or 'xpath-expression-ref' is required.");
		if (hasChild) {
			Element xpathExpressionElement = (Element) xPathExpressionNodes.item(0);
			builder.addConstructorArgValue(xpathExpressionElement.getAttribute("expression"));
			XPathExpressionParser.parseAndPopulateNamespaceMap(xpathExpressionElement, parserContext, builder);
		}
		else {
			builder.addConstructorArgReference(xPathExpressionRef);
		}
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "doc-builder-factory",
				"documentBuilder");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "create-documents");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "apply-sequence");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "iterator");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "output-properties");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "send-timeout");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "discard-channel", "discardChannelName");
		return builder;
	}

}
