/*
 * Copyright © 2013 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2013-present the original author or authors.
 */

package org.springframework.integration.kafka.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractOutboundChannelAdapterParser;
import org.springframework.integration.kafka.outbound.KafkaProducerMessageHandler;

/**
 * Parser for the outbound channel adapter.
 *
 * @author Soby Chacko
 * @author Artem Bilan
 * @author Gary Russell
 * @author Biju Kunjummen
 *
 * @since 5.4
 *
 */
public class KafkaOutboundChannelAdapterParser extends AbstractOutboundChannelAdapterParser {

	@Override
	protected AbstractBeanDefinition parseConsumer(final Element element, final ParserContext parserContext) {
		final BeanDefinitionBuilder builder =
				BeanDefinitionBuilder.genericBeanDefinition(KafkaProducerMessageHandler.class);

		KafkaParsingUtils.commonOutboundProperties(element, parserContext, builder);
		return builder.getBeanDefinition();
	}

}
