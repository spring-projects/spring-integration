/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.websocket.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.websocket.inbound.WebSocketInboundChannelAdapter;

/**
 * The {@link AbstractChannelAdapterParser} implementation for
 * the {@code <int-websocket:inbound-channel-adapter/>} element.
 *
 * @author Artem Bilan
 * @since 4.1
 */
public class WebSocketInboundChannelAdapterParser extends AbstractChannelAdapterParser {

	@Override
	protected AbstractBeanDefinition doParse(Element element, ParserContext parserContext, String channelName) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(WebSocketInboundChannelAdapter.class);
		WebSocketAdapterParsingUtils.configureWebSocketAdapter(builder, parserContext, element);
		builder.addPropertyReference("outputChannel", channelName);
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "error-channel");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "send-timeout");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "payload-type");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "use-broker");
		return builder.getBeanDefinition();
	}

}
