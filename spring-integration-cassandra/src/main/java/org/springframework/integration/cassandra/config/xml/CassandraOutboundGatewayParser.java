/*
 * Copyright © 2022 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2022-present the original author or authors.
 */

package org.springframework.integration.cassandra.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.cassandra.outbound.CassandraMessageHandler;
import org.springframework.integration.config.xml.AbstractConsumerEndpointParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;

/**
 * The parser for the {@code <int-cassandra:outbound-gateway>}.
 *
 * @author Filippo Balicchia
 * @author Artem Bilan
 *
 * @since 6.0
 */
public class CassandraOutboundGatewayParser extends AbstractConsumerEndpointParser {

	@Override
	protected String getInputChannelAttributeName() {
		return "request-channel";
	}

	@Override
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(CassandraMessageHandler.class);
		builder.addPropertyValue("producesReply", true);
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "reply-channel", "outputChannel");
		CassandraParserUtils.processOutboundTypeAttributes(element, parserContext, builder);
		return builder;
	}

}
