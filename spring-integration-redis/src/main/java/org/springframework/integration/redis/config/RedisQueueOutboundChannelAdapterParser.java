/*
 * Copyright © 2013 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2013-present the original author or authors.
 */

package org.springframework.integration.redis.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractOutboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.redis.outbound.RedisQueueOutboundChannelAdapter;
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;int-redis:queue-outbound-channel-adapter&gt; element.
 *
 * @author Artem Bilan
 * @author Rainer Frey
 * @since 3.0
 */
public class RedisQueueOutboundChannelAdapterParser extends AbstractOutboundChannelAdapterParser {

	@Override
	protected AbstractBeanDefinition parseConsumer(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(RedisQueueOutboundChannelAdapter.class);
		BeanDefinition queueExpression = IntegrationNamespaceUtils
				.createExpressionDefinitionFromValueOrExpression("queue", "queue-expression", parserContext, element, true);
		builder.addConstructorArgValue(queueExpression);

		String connectionFactory = element.getAttribute("connection-factory");
		if (!StringUtils.hasText(connectionFactory)) {
			connectionFactory = "redisConnectionFactory";
		}
		builder.addConstructorArgReference(connectionFactory);

		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "extract-payload");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "serializer");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "left-push");

		return builder.getBeanDefinition();
	}

}
