/*
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jmx.config;

import org.w3c.dom.Element;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractPollingInboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.jmx.inbound.AttributePollingMessageSource;

/**
 * @author Mark Fisher
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class AttributePollingChannelAdapterParser extends AbstractPollingInboundChannelAdapterParser {

	@Override
	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

	@Override
	protected BeanMetadataElement parseSource(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(AttributePollingMessageSource.class);
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "server", "server");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "object-name");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "attribute-name");
		return builder.getBeanDefinition();
	}

}
