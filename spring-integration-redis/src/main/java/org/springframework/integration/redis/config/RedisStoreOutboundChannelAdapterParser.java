/*
 * Copyright © 2007 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2007-present the original author or authors.
 */

package org.springframework.integration.redis.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.TypedStringValue;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractOutboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.redis.outbound.RedisStoreWritingMessageHandler;
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;redis:store-outbound-channel-adapter&gt; element.
 *
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Mark Fisher
 * @author Artem Bilan
 *
 * @since 2.2
 */
public class RedisStoreOutboundChannelAdapterParser extends AbstractOutboundChannelAdapterParser {

	@Override
	protected AbstractBeanDefinition parseConsumer(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder =
				BeanDefinitionBuilder.genericBeanDefinition(RedisStoreWritingMessageHandler.class);

		String redisTemplateRef = element.getAttribute("redis-template");
		String connectionFactory = element.getAttribute("connection-factory");
		if (StringUtils.hasText(redisTemplateRef) && StringUtils.hasText(connectionFactory)) {
			parserContext.getReaderContext().error("Only one of 'redis-template' or 'connection-factory'" +
					" is allowed.", element);
		}
		if (StringUtils.hasText(redisTemplateRef)) {
			builder.addConstructorArgReference(redisTemplateRef);
		}
		else {
			if (!StringUtils.hasText(connectionFactory)) {
				connectionFactory = "redisConnectionFactory";
			}
			builder.addConstructorArgReference(connectionFactory);
		}

		boolean hasKey = element.hasAttribute("key");
		boolean hasKeyExpression = element.hasAttribute("key-expression");
		if (hasKey && hasKeyExpression) {
			parserContext.getReaderContext().error(
					"At most one of 'key' or 'key-expression' is allowed.", element);
		}
		if (hasKey) {
			builder.addPropertyValue("key", new TypedStringValue(element.getAttribute("key")));
		}
		if (hasKeyExpression) {
			builder.addPropertyValue("keyExpressionString", element.getAttribute("key-expression"));
		}

		String mapKeyExpression = element.getAttribute("map-key-expression");
		if (StringUtils.hasText(mapKeyExpression)) {
			builder.addPropertyValue("mapKeyExpressionString", mapKeyExpression);
		}

		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "collection-type");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "extract-payload-elements");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "zset-increment-expression",
				"zsetIncrementExpressionString");
		return builder.getBeanDefinition();
	}

}
