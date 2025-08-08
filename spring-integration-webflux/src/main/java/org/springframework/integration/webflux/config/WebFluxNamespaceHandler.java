/*
 * Copyright © 2017 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2017-present the original author or authors.
 */

package org.springframework.integration.webflux.config;

import org.springframework.integration.config.xml.AbstractIntegrationNamespaceHandler;

/**
 * Namespace handler for Spring Integration's <em>webflux</em> namespace.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
public class WebFluxNamespaceHandler extends AbstractIntegrationNamespaceHandler {

	public void init() {
		registerBeanDefinitionParser("inbound-channel-adapter", new WebFluxInboundEndpointParser(false));
		registerBeanDefinitionParser("inbound-gateway", new WebFluxInboundEndpointParser(true));
		registerBeanDefinitionParser("outbound-channel-adapter", new WebFluxOutboundChannelAdapterParser());
		registerBeanDefinitionParser("outbound-gateway", new WebFluxOutboundGatewayParser());
	}

}
