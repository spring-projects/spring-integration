/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.redis.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractConsumerEndpointParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.redis.outbound.RedisQueueOutboundGateway;
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;int-redis:queue-outbound-channel-adapter&gt; element.
 *
 * @author Artem Bilan
 * @author David Liu
 * @since 3.0
 */
public class RedisQueueOutboundGatewayParser extends AbstractConsumerEndpointParser {

	@Override
	protected String getInputChannelAttributeName() {
		return "request-channel";
	}

	@Override
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(RedisQueueOutboundGateway.class);
		builder.addConstructorArgValue(element.getAttribute("queue"));
		String connectionFactory = element.getAttribute("connection-factory");
		if (!StringUtils.hasText(connectionFactory)) {
			connectionFactory = "redisConnectionFactory";
		}
		builder.addConstructorArgReference(connectionFactory);
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "reply-channel", "outputChannel");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "extract-payload");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "serializer");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "reply-timeout", "receiveTimeout");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "requires-reply");
		return builder;
	}

}
