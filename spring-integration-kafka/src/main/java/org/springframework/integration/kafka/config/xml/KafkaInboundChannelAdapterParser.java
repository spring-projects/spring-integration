/*
 * Copyright © 2019 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2019-present the original author or authors.
 */

package org.springframework.integration.kafka.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractPollingInboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.kafka.inbound.KafkaMessageSource;
import org.springframework.util.StringUtils;

/**
 * Parser for the inbound channel adapter.
 *
 * @author Gary Russell
 * @author Anshul Mehra
 *
 * @since 5.4
 *
 */
public class KafkaInboundChannelAdapterParser extends AbstractPollingInboundChannelAdapterParser {

	@Override
	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

	@Override
	protected BeanMetadataElement parseSource(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(KafkaMessageSource.class);
		builder.addConstructorArgReference(element.getAttribute("consumer-factory"));
		builder.addConstructorArgReference(element.getAttribute("consumer-properties"));
		String attribute = element.getAttribute("ack-factory");
		if (StringUtils.hasText(attribute)) {
			builder.addConstructorArgReference(attribute);
		}
		attribute = element.getAttribute("allow-multi-fetch");
		if (StringUtils.hasText(attribute)) {
			builder.addConstructorArgValue(attribute);
		}

		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "message-converter");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "payload-type");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "raw-header", "rawMessageHeader");
		return builder.getBeanDefinition();
	}

}
