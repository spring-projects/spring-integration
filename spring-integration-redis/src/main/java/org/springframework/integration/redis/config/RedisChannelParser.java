/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.redis.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractChannelParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.redis.channel.SubscribableRedisChannel;
import org.springframework.util.StringUtils;

/**
 * Parser for the 'channel' and 'publish-subscribe-channel' elements of the
 * Spring Integration Redis namespace.
 *
 * @author Oleg Zhurakusky
 * @author Artem Bilan
 * @author Gary Russell
 * @since 2.1
 */
public class RedisChannelParser extends AbstractChannelParser {

	@Override
	protected BeanDefinitionBuilder buildBeanDefinition(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(SubscribableRedisChannel.class);
		String connectionFactory = element.getAttribute("connection-factory");
		if (!StringUtils.hasText(connectionFactory)) {
			connectionFactory = "redisConnectionFactory";
		}
		builder.addConstructorArgReference(connectionFactory);
		String topicName = element.getAttribute("topic-name");
		builder.addConstructorArgValue(topicName);

		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "task-executor");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "message-converter");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "serializer");
		// The following 2 attributes should be added once configurable on the RedisMessageListenerContainer
		// IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "phase");
		// IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "auto-startup");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "max-subscribers");

		return builder;
	}

}
