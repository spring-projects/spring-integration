/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
 */

package org.springframework.integration.event.config;

import org.springframework.integration.config.xml.AbstractIntegrationNamespaceHandler;

/**
 * Namespace handler for Spring Integration's 'event' namespace.
 *
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class EventNamespaceHandler extends AbstractIntegrationNamespaceHandler {

	public void init() {
		registerBeanDefinitionParser("inbound-channel-adapter", new EventInboundChannelAdapterParser());
		registerBeanDefinitionParser("outbound-channel-adapter", new EventOutboundChannelAdapterParser());
	}

}
