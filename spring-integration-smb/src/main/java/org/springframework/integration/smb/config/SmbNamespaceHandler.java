/*
 * Copyright © 2012 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.integration.smb.config;

import org.springframework.integration.config.xml.AbstractIntegrationNamespaceHandler;

/**
 * Provides namespace support for using SMB.
 *
 * @author Markus Spann
 * @author Artem Bilan
 * @author Gregory Bragg
 *
 * @since 6.0
 */
public class SmbNamespaceHandler extends AbstractIntegrationNamespaceHandler {

	public void init() {
		registerBeanDefinitionParser("inbound-channel-adapter", new SmbInboundChannelAdapterParser());
		registerBeanDefinitionParser("inbound-streaming-channel-adapter", new SmbStreamingInboundChannelAdapterParser());
		registerBeanDefinitionParser("outbound-channel-adapter", new SmbOutboundChannelAdapterParser());
		registerBeanDefinitionParser("outbound-gateway", new SmbOutboundGatewayParser());
	}

}
