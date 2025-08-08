/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.websocket.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractOutboundChannelAdapterParser;
import org.springframework.integration.websocket.outbound.WebSocketOutboundMessageHandler;

/**
 * The {@link AbstractOutboundChannelAdapterParser} implementation for
 * the {@code <websocket:outbound-channel-adapter/>} element.
 *
 * @author Artem Bilan
 * @since 4.1
 */
public class WebSocketOutboundMessageHandlerParser extends AbstractOutboundChannelAdapterParser {

	@Override
	protected AbstractBeanDefinition parseConsumer(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(WebSocketOutboundMessageHandler.class);
		WebSocketAdapterParsingUtils.configureWebSocketAdapter(builder, parserContext, element);
		return builder.getBeanDefinition();
	}

}
