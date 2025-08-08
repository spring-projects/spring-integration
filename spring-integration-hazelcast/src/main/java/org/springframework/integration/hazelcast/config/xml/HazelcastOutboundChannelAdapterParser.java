/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.hazelcast.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractOutboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.hazelcast.outbound.HazelcastCacheWritingMessageHandler;

/**
 * Hazelcast Outbound Channel Adapter Parser for
 * {@code <int-hazelcast:inbound-channel-adapter />}.
 *
 * @author Eren Avsarogullari
 * @since 6.0
 */
public class HazelcastOutboundChannelAdapterParser extends AbstractOutboundChannelAdapterParser {

	private static final String CACHE_ATTRIBUTE = "cache";

	private static final String CACHE_EXPRESSION_ATTRIBUTE = "cache-expression";

	private static final String KEY_EXPRESSION_ATTRIBUTE = "key-expression";

	private static final String EXTRACT_PAYLOAD_ATTRIBUTE = "extract-payload";

	private static final String DISTRIBUTED_OBJECT = "distributedObject";

	@Override
	protected AbstractBeanDefinition parseConsumer(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder
				.genericBeanDefinition(HazelcastCacheWritingMessageHandler.class);

		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, CACHE_ATTRIBUTE, DISTRIBUTED_OBJECT);
		BeanDefinition cacheExpressionDef =
				IntegrationNamespaceUtils.createExpressionDefIfAttributeDefined(CACHE_EXPRESSION_ATTRIBUTE, element);
		if (cacheExpressionDef != null) {
			builder.addPropertyValue("cacheExpression", cacheExpressionDef);
		}

		BeanDefinition keyExpressionDef =
				IntegrationNamespaceUtils.createExpressionDefIfAttributeDefined(KEY_EXPRESSION_ATTRIBUTE, element);
		if (keyExpressionDef != null) {
			builder.addPropertyValue("keyExpression", keyExpressionDef);
		}

		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, EXTRACT_PAYLOAD_ATTRIBUTE);

		return builder.getBeanDefinition();
	}

}
