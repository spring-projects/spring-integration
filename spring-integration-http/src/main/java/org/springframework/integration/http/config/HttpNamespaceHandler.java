/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.http.config;

import org.springframework.integration.config.xml.AbstractIntegrationNamespaceHandler;

/**
 * Namespace handler for Spring Integration's <em>http</em> namespace.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Shiliang Li
 *
 * @since 1.0.2
 */
public class HttpNamespaceHandler extends AbstractIntegrationNamespaceHandler {

	public void init() {
		registerBeanDefinitionParser("inbound-channel-adapter", new HttpInboundEndpointParser(false));
		registerBeanDefinitionParser("inbound-gateway", new HttpInboundEndpointParser(true));
		registerBeanDefinitionParser("outbound-channel-adapter", new HttpOutboundChannelAdapterParser());
		registerBeanDefinitionParser("outbound-gateway", new HttpOutboundGatewayParser());
		registerBeanDefinitionParser("graph-controller", new IntegrationGraphControllerParser());
	}

}
