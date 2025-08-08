/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.ftp.config;

import org.springframework.integration.config.xml.AbstractIntegrationNamespaceHandler;

/**
 * Provides namespace support for using FTP
 * <p>
 * This is *heavily* influenced by the good work done by Iwein before.
 *
 * @author Josh Long
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @since 2.0
 */
public class FtpNamespaceHandler extends AbstractIntegrationNamespaceHandler {

	@Override
	public void init() {
		registerBeanDefinitionParser("inbound-channel-adapter", new FtpInboundChannelAdapterParser());
		registerBeanDefinitionParser("inbound-streaming-channel-adapter",
				new FtpStreamingInboundChannelAdapterParser());
		registerBeanDefinitionParser("outbound-channel-adapter", new FtpOutboundChannelAdapterParser());
		registerBeanDefinitionParser("outbound-gateway", new FtpOutboundGatewayParser());
	}

}
