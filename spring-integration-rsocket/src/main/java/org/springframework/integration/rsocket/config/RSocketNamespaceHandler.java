/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.rsocket.config;

import org.springframework.integration.config.xml.AbstractIntegrationNamespaceHandler;

/**
 * Namespace handler for Spring Integration XML configuration for <em>RSocket</em> support.
 *
 * @author Artem Bilan
 *
 * @since 5.2
 */
public class RSocketNamespaceHandler extends AbstractIntegrationNamespaceHandler {

	public void init() {
		registerBeanDefinitionParser("inbound-gateway", new RSocketInboundGatewayParser());
		registerBeanDefinitionParser("outbound-gateway", new RSocketOutboundGatewayParser());
	}

}
