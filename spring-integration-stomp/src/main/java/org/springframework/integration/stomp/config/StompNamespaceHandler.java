/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
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
