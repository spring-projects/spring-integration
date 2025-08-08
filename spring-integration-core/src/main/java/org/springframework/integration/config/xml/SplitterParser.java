/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.SplitterFactoryBean;

/**
 * Parser for the &lt;splitter/&gt; element.
 *
 * @author Mark Fisher
 * @author Iwein Fuld
 * @author Gary Russell
 * @author Artem Bilan
 */
public class SplitterParser extends AbstractDelegatingConsumerEndpointParser {

	@Override
	String getFactoryBeanClassName() {
		return SplitterFactoryBean.class.getName();
	}

	@Override
	boolean hasDefaultOption() {
		return true;
	}

	@Override
	void postProcess(BeanDefinitionBuilder builder, Element element, ParserContext parserContext) {
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "apply-sequence");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "delimiters");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "discard-channel", "discardChannelName");
	}

}
