/*
 * Copyright © 2013 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2013-present the original author or authors.
 */

package org.springframework.integration.redis.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.redis.inbound.RedisQueueMessageDrivenEndpoint;
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;queue-inbound-channel-adapter&gt; element of the 'redis' namespace.
 *
 * @author Artem Bilan
 * @author Rainer Frey
 * @since 3.0
 */
public class RedisQueueInboundChannelAdapterParser extends AbstractChannelAdapterParser {

	@Override
	protected AbstractBeanDefinition doParse(Element element, ParserContext parserContext, String channelName) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(RedisQueueMessageDrivenEndpoint.class);
		builder.addConstructorArgValue(element.getAttribute("queue"));

		String connectionFactory = element.getAttribute("connection-factory");
		if (!StringUtils.hasText(connectionFactory)) {
			connectionFactory = "redisConnectionFactory";
		}
		builder.addConstructorArgReference(connectionFactory);

		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "serializer", true);
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "task-executor");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "error-channel");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "expect-message");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "receive-timeout");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "recovery-interval");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "right-pop");
		builder.addPropertyReference("outputChannel", channelName);

		return builder.getBeanDefinition();
	}

}
