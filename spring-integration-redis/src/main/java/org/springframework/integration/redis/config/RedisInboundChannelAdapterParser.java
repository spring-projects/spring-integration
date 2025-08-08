/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.redis.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.redis.inbound.RedisInboundChannelAdapter;
import org.springframework.util.StringUtils;

/**
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 * @author Venil Noronha
 *
 * @since 2.1
 */
public class RedisInboundChannelAdapterParser extends AbstractChannelAdapterParser {

	@Override
	protected AbstractBeanDefinition doParse(Element element, ParserContext parserContext, String channelName) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(RedisInboundChannelAdapter.class);
		String connectionFactory = element.getAttribute("connection-factory");
		if (!StringUtils.hasText(connectionFactory)) {
			connectionFactory = "redisConnectionFactory";
		}
		builder.addConstructorArgReference(connectionFactory);
		builder.addPropertyReference("outputChannel", channelName);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "topics");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "topic-patterns");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "error-channel");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "message-converter");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "serializer", true);
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "task-executor");

		return builder.getBeanDefinition();
	}

}
