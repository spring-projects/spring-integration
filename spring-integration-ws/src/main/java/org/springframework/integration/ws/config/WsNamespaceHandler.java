/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.ws.config;

import org.springframework.integration.config.xml.AbstractIntegrationNamespaceHandler;

/**
 * @author Mark Fisher
 * @author Iwein Fuld
 */
public class WsNamespaceHandler extends AbstractIntegrationNamespaceHandler {

	public void init() {
		this.registerBeanDefinitionParser("outbound-gateway", new WebServiceOutboundGatewayParser());
		this.registerBeanDefinitionParser("inbound-gateway", new WebServiceInboundGatewayParser());
		this.registerBeanDefinitionParser("header-enricher", new WebServiceHeaderEnricherParser());
	}

}
