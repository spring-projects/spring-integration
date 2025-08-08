/*
 * Copyright © 2019 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2019-present the original author or authors.
 */

package org.springframework.integration.kafka.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.integration.config.xml.AbstractInboundGatewayParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.kafka.inbound.KafkaInboundGateway;

/**
 * Inbound gateway parser.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 5.4
 *
 */
public class KafkaInboundGatewayParser extends AbstractInboundGatewayParser {

	@Override
	protected Class<?> getBeanClass(Element element) {
		return KafkaInboundGateway.class;
	}

	@Override
	protected void doPostProcess(BeanDefinitionBuilder builder, Element element) {
		builder.addConstructorArgReference(element.getAttribute("listener-container"));
		builder.addConstructorArgReference(element.getAttribute("kafka-template"));
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "error-channel");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "message-converter");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "error-message-strategy");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "retry-template");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "recovery-callback");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element,
				"on-partitions-assigned-seek-callback");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "bind-source-record");
	}

	@Override
	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

	@Override
	protected boolean isEligibleAttribute(String attributeName) {
		return super.isEligibleAttribute(attributeName)
				&& !attributeName.equals("listener-container")
				&& !attributeName.equals("kafka-template");
	}

}
