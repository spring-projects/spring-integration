/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
