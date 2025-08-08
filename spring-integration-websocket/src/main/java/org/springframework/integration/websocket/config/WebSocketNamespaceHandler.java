/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.websocket.config;

import org.springframework.integration.config.xml.AbstractIntegrationNamespaceHandler;

/**
 * @author Artem Bilan
 * @since 4.1
 */
public class WebSocketNamespaceHandler extends AbstractIntegrationNamespaceHandler {

	public void init() {
		this.registerBeanDefinitionParser("server-container", new ServerWebSocketContainerParser());
		this.registerBeanDefinitionParser("client-container", new ClientWebSocketContainerParser());
		this.registerBeanDefinitionParser("inbound-channel-adapter", new WebSocketInboundChannelAdapterParser());
		this.registerBeanDefinitionParser("outbound-channel-adapter", new WebSocketOutboundMessageHandlerParser());
	}

}
