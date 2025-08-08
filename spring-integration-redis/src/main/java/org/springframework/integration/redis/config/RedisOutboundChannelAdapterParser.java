/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.redis.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractOutboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.redis.outbound.RedisPublishingMessageHandler;
import org.springframework.util.StringUtils;

/**
 * Parser for the {@code <outbound-channel-adapter/>} component.
 *
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Artem Bilan
 * @since 2.1
 */
public class RedisOutboundChannelAdapterParser extends AbstractOutboundChannelAdapterParser {

	@Override
	protected AbstractBeanDefinition parseConsumer(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(RedisPublishingMessageHandler.class);
		String connectionFactory = element.getAttribute("connection-factory");
		if (!StringUtils.hasText(connectionFactory)) {
			connectionFactory = "redisConnectionFactory";
		}
		builder.addConstructorArgReference(connectionFactory);

		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "message-converter");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "serializer");

		BeanDefinition topicExpression = IntegrationNamespaceUtils
				.createExpressionDefinitionFromValueOrExpression("topic", "topic-expression", parserContext, element, true);
		builder.addPropertyValue("topicExpression", topicExpression);

		return builder.getBeanDefinition();
	}

}
