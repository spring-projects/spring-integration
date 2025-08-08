/*
 * Copyright © 2007 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2007-present the original author or authors.
 */

package org.springframework.integration.mongodb.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractOutboundChannelAdapterParser;
import org.springframework.integration.mongodb.outbound.MongoDbStoringMessageHandler;

/**
 * Parser for Mongodb store outbound adapters.
 *
 * @author Oleg Zhurakousky
 *
 * @since 2.2
 */
public class MongoDbOutboundChannelAdapterParser extends AbstractOutboundChannelAdapterParser {

	@Override
	protected AbstractBeanDefinition parseConsumer(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(MongoDbStoringMessageHandler.class);

		// Will parse and validate 'mongodb-template', 'mongodb-factory',
		// 'collection-name', 'collection-name-expression' and 'mongo-converter'
		MongoParserUtils.processCommonAttributes(element, parserContext, builder);

		return builder.getBeanDefinition();
	}

}
