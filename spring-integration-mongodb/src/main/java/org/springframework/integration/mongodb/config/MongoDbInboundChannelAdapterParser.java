/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.mongodb.config;

import org.w3c.dom.Element;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractPollingInboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.mongodb.inbound.MongoDbMessageSource;

/**
 * Parser for MongoDb store inbound adapters.
 *
 * @author Amol Nayak
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 *
 * @since 2.2
 */
public class MongoDbInboundChannelAdapterParser extends AbstractPollingInboundChannelAdapterParser {

	@Override
	protected BeanMetadataElement parseSource(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(MongoDbMessageSource.class);

		// Will parse and validate 'mongodb-template', 'mongodb-factory',
		// 'collection-name', 'collection-name-expression' and 'mongo-converter'
		MongoParserUtils.processCommonAttributes(element, parserContext, builder);

		BeanDefinition queryExpressionDef =
				IntegrationNamespaceUtils.createExpressionDefinitionFromValueOrExpression("query", "query-expression",
						parserContext, element, true);

		builder.addConstructorArgValue(queryExpressionDef);

		BeanDefinition expressionDef =
				IntegrationNamespaceUtils.createExpressionDefinitionFromValueOrExpression("update", "update-expression",
						parserContext, element, false);
		builder.addPropertyValue("updateExpression", expressionDef);

		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "entity-class");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "expect-single-result");

		return builder.getBeanDefinition();
	}

}
