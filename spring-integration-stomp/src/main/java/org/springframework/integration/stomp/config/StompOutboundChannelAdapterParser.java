/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.stomp.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractOutboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.stomp.outbound.StompMessageHandler;

/**
 * The {@link AbstractOutboundChannelAdapterParser} implementation for
 * the {@code <stomp:outbound-channel-adapter/>} element.
 *
 * @author Artem Bilan
 * @since 4.2
 */
public class StompOutboundChannelAdapterParser extends AbstractOutboundChannelAdapterParser {

	@Override
	protected AbstractBeanDefinition parseConsumer(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(StompMessageHandler.class);
		StompAdapterParserUtils.configureStompAdapter(builder, parserContext, element);
		BeanDefinition expressionDef =
				IntegrationNamespaceUtils.createExpressionDefinitionFromValueOrExpression("destination",
						"destination-expression", parserContext, element, false);
		if (expressionDef != null) {
			builder.addPropertyValue("destinationExpression", expressionDef);
		}
		return builder.getBeanDefinition();

	}

}
