/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.redis.config;

import org.w3c.dom.Element;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractPollingInboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.redis.inbound.RedisStoreMessageSource;
import org.springframework.util.StringUtils;

/**
 * Parser for Redis store inbound adapters
 *
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @since 2.2
 */
public class RedisStoreInboundChannelAdapterParser extends AbstractPollingInboundChannelAdapterParser {

	@Override
	protected BeanMetadataElement parseSource(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(RedisStoreMessageSource.class);
		String redisTemplate = element.getAttribute("redis-template");
		String connectionFactory = element.getAttribute("connection-factory");
		if (StringUtils.hasText(redisTemplate) && StringUtils.hasText(connectionFactory)) {
			parserContext.getReaderContext().error("Only one of '" + redisTemplate + "' or '"
					+ connectionFactory + "' is allowed.", element);
		}
		if (StringUtils.hasText(redisTemplate)) {
			builder.addConstructorArgReference(redisTemplate);
		}
		else {
			if (!StringUtils.hasText(connectionFactory)) {
				connectionFactory = "redisConnectionFactory";
			}
			builder.addConstructorArgReference(connectionFactory);
		}
		boolean atLeastOneRequired = true;
		BeanDefinition expressionDef =
				IntegrationNamespaceUtils.createExpressionDefinitionFromValueOrExpression("key", "key-expression",
						parserContext, element, atLeastOneRequired);
		builder.addConstructorArgValue(expressionDef);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "collection-type");

		return builder.getBeanDefinition();
	}

}
