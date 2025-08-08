/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.hazelcast.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractPollingInboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.hazelcast.inbound.HazelcastDistributedSQLMessageSource;
import org.springframework.util.StringUtils;

/**
 * Hazelcast Distributed SQL Inbound Channel Adapter Parser parses
 * {@code <int-hazelcast:ds-inbound-channel-adapter/>} configuration.
 *
 * @author Eren Avsarogullari
 * @since 6.0
 */
public class HazelcastDistributedSQLInboundChannelAdapterParser extends AbstractPollingInboundChannelAdapterParser {

	private static final String CACHE_ATTRIBUTE = "cache";

	private static final String DISTRIBUTED_SQL_ATTRIBUTE = "distributed-sql";

	private static final String ITERATION_TYPE_ATTRIBUTE = "iteration-type";

	@Override
	protected BeanMetadataElement parseSource(Element element, ParserContext parserContext) {
		if (!StringUtils.hasText(element.getAttribute(CACHE_ATTRIBUTE))) {
			parserContext.getReaderContext().error("'" + CACHE_ATTRIBUTE + "' attribute is required.", element);
		}
		else if (!StringUtils.hasText(element.getAttribute(DISTRIBUTED_SQL_ATTRIBUTE))) {
			parserContext.getReaderContext().error("'" + DISTRIBUTED_SQL_ATTRIBUTE + "' attribute is required.",
					element);
		}
		else if (!StringUtils.hasText(element.getAttribute(ITERATION_TYPE_ATTRIBUTE))) {
			parserContext.getReaderContext().error("'" + ITERATION_TYPE_ATTRIBUTE + "' attribute is required.",
					element);
		}

		BeanDefinitionBuilder builder = BeanDefinitionBuilder
				.genericBeanDefinition(HazelcastDistributedSQLMessageSource.class.getName());

		builder.addConstructorArgReference(element.getAttribute(CACHE_ATTRIBUTE));
		builder.addConstructorArgValue(element.getAttribute(DISTRIBUTED_SQL_ATTRIBUTE));

		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, ITERATION_TYPE_ATTRIBUTE);

		return builder.getBeanDefinition();
	}

}
