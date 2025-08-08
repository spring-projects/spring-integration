/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.FilterFactoryBean;
import org.springframework.util.xml.DomUtils;

/**
 * Parser for the &lt;filter/&gt; element.
 *
 * @author Mark Fisher
 * @author Gary Russell
 */
public class FilterParser extends AbstractDelegatingConsumerEndpointParser {

	@Override
	String getFactoryBeanClassName() {
		return FilterFactoryBean.class.getName();
	}

	@Override
	boolean hasDefaultOption() {
		return false;
	}

	@Override
	void postProcess(BeanDefinitionBuilder builder, Element element, ParserContext parserContext) {
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "discard-channel");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "throw-exception-on-rejection");
		Element adviceChainElement = DomUtils.getChildElementByTagName(element,
				IntegrationNamespaceUtils.REQUEST_HANDLER_ADVICE_CHAIN);
		if (adviceChainElement != null) {
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, adviceChainElement, "discard-within-advice");
		}
	}

}
