/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.handler.BridgeHandler;

/**
 * Parser for the &lt;bridge&gt; element.
 *
 * @author Mark Fisher
 */
public class BridgeParser extends AbstractConsumerEndpointParser {

	@Override
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(BridgeHandler.class);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "send-timeout");
		return builder;
	}

}
