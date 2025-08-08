/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jmx.config;

import org.w3c.dom.Element;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractPollingInboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;

/**
 * @author Mark Fisher
 * @since 2.0
 */
public class AttributePollingChannelAdapterParser extends AbstractPollingInboundChannelAdapterParser {

	@Override
	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

	@Override
	protected BeanMetadataElement parseSource(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(
				"org.springframework.integration.jmx.AttributePollingMessageSource");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "server", "server");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "object-name");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "attribute-name");
		return builder.getBeanDefinition();
	}

}
