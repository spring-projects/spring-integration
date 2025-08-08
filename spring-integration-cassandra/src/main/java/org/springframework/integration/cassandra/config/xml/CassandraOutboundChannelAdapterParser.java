/*
 * Copyright © 2022 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2022-present the original author or authors.
 */

package org.springframework.integration.cassandra.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.cassandra.outbound.CassandraMessageHandler;
import org.springframework.integration.config.xml.AbstractOutboundChannelAdapterParser;

/**
 * The parser for the {@code <int-cassandra:outbound-channel-adapter>}.
 *
 * @author Filippo Balicchia
 * @author Artem Bilan
 *
 * @since 6.0
 */
public class CassandraOutboundChannelAdapterParser extends AbstractOutboundChannelAdapterParser {

	@Override
	protected AbstractBeanDefinition parseConsumer(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(CassandraMessageHandler.class);
		builder.addPropertyValue("producesReply", false);
		CassandraParserUtils.processOutboundTypeAttributes(element, parserContext, builder);
		return builder.getBeanDefinition();
	}

}
