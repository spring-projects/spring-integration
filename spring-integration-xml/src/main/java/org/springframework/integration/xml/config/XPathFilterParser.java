/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.xml.config;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.FilterFactoryBean;
import org.springframework.integration.config.xml.AbstractConsumerEndpointParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.xml.selector.BooleanTestXPathMessageSelector;
import org.springframework.integration.xml.selector.RegexTestXPathMessageSelector;
import org.springframework.integration.xml.selector.StringValueTestXPathMessageSelector;
import org.springframework.util.Assert;
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;xpath-filter&gt; element.
 *
 * @author Mark Fisher
 * @since 2.1
 */
public class XPathFilterParser extends AbstractConsumerEndpointParser {

	private final XPathExpressionParser xpathParser = new XPathExpressionParser();

	@Override
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
		final BeanDefinitionBuilder selectorBuilder = BeanDefinitionBuilder.genericBeanDefinition();
		selectorBuilder.getBeanDefinition().setBeanClass(BooleanTestXPathMessageSelector.class);
		this.configureXPathExpression(element, selectorBuilder, parserContext);
		if (element.hasAttribute("match-value")) {
			selectorBuilder.addConstructorArgValue(element.getAttribute("match-value"));
			String matchType = element.getAttribute("match-type");
			if ("exact".equals(matchType)) {
				selectorBuilder.getBeanDefinition().setBeanClass(StringValueTestXPathMessageSelector.class);
			}
			else if ("case-insensitive".equals(matchType)) {
				selectorBuilder.getBeanDefinition().setBeanClass(StringValueTestXPathMessageSelector.class);
				selectorBuilder.addPropertyValue("caseSensitive", false);
			}
			else if ("regex".equals(matchType)) {
				selectorBuilder.getBeanDefinition().setBeanClass(RegexTestXPathMessageSelector.class);
			}
		}
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(FilterFactoryBean.class);
		builder.addPropertyValue("targetObject", selectorBuilder.getBeanDefinition());
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "discard-channel");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "throw-exception-on-rejection");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "send-timeout");
		return builder;
	}

	private void configureXPathExpression(Element element, BeanDefinitionBuilder selectorBuilder, ParserContext parserContext) {
		String xPathExpressionRef = element.getAttribute("xpath-expression-ref");
		NodeList xPathExpressionNodes = element.getElementsByTagNameNS(element.getNamespaceURI(), "xpath-expression");
		Assert.isTrue(xPathExpressionNodes.getLength() <= 1, "At most one xpath-expression child may be specified.");
		boolean xPathExpressionChildPresent = xPathExpressionNodes.getLength() == 1;
		boolean xPathReferencePresent = StringUtils.hasText(xPathExpressionRef);
		Assert.isTrue(xPathExpressionChildPresent ^ xPathReferencePresent,
				"Exactly one of 'xpath-expression' or 'xpath-expression-ref' is required.");
		if (xPathExpressionChildPresent) {
			BeanDefinition beanDefinition = this.xpathParser.parse((Element) xPathExpressionNodes.item(0), parserContext);
			selectorBuilder.addConstructorArgValue(beanDefinition);
		}
		else {
			selectorBuilder.addConstructorArgReference(xPathExpressionRef);
		}
	}

}
