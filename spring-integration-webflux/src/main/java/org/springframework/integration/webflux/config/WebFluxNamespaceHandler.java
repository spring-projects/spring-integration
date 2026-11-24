/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
