/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.kafka.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.kafka.inbound.KafkaMessageDrivenChannelAdapter;
import org.springframework.util.StringUtils;

/**
 *
 * Parser for the message driven channel adapter.
 *
 * @author Artem Bilan
 * @author Gary Russell
 *
 * @since 5.4
 */
public class KafkaMessageDrivenChannelAdapterParser extends AbstractChannelAdapterParser {

	@Override
	protected AbstractBeanDefinition doParse(Element element, ParserContext parserContext, String channelName) {
		BeanDefinitionBuilder builder =
				BeanDefinitionBuilder.genericBeanDefinition(KafkaMessageDrivenChannelAdapter.class);

		String container = element.getAttribute("listener-container");
		if (StringUtils.hasText(container)) {
			builder.addConstructorArgReference(container);
		}
		else {
			parserContext.getReaderContext().error("The 'listener-container' attribute is required.", element);
		}
		builder.addConstructorArgValue(element.getAttribute("mode"));

		builder.addPropertyReference("outputChannel", channelName);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "send-timeout");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "error-channel");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "message-converter");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "payload-type");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "error-message-strategy");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "retry-template");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "recovery-callback");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "record-filter-strategy");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element,
				"on-partitions-assigned-seek-callback");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "ack-discarded");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "filter-in-retry");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "bind-source-record");

		return builder.getBeanDefinition();
	}

}
