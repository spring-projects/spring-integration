/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.stomp.config;

import org.springframework.integration.config.xml.AbstractIntegrationNamespaceHandler;

/**
 * @author Artem Bilan
 * @since 4.2
 */
public class StompNamespaceHandler extends AbstractIntegrationNamespaceHandler {

	public void init() {
		registerBeanDefinitionParser("inbound-channel-adapter", new StompInboundChannelAdapterParser());
		registerBeanDefinitionParser("outbound-channel-adapter", new StompOutboundChannelAdapterParser());
	}

}
