/*
 * Copyright © 2019 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2019-present the original author or authors.
 */

package org.springframework.integration.kafka.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractConsumerEndpointParser;
import org.springframework.integration.kafka.outbound.KafkaProducerMessageHandler;

/**
 * Parser for the outbound gateway.
 *
 * @author Gary Russell
 *
 * @since 5.4
 *
 */
public class KafkaOutboundGatewayParser extends AbstractConsumerEndpointParser {

	@Override
	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

	@Override
	protected String getInputChannelAttributeName() {
		return "request-channel";
	}

	@Override
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(KafkaProducerMessageHandler.class);
		KafkaParsingUtils.commonOutboundProperties(element, parserContext, builder);
		return builder;
	}

}
