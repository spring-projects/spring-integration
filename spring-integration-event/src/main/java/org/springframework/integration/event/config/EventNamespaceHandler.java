/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
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
