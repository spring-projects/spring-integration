/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.ip.config;

import org.springframework.integration.config.xml.AbstractIntegrationNamespaceHandler;

/**
 * Namespace handler for Spring Integration's <em>ip</em> namespace.
 *
 * @author Gary Russell
 * @since 2.0
 */
public class IpNamespaceHandler extends AbstractIntegrationNamespaceHandler {

	public void init() {
		this.registerBeanDefinitionParser("udp-inbound-channel-adapter", new UdpInboundChannelAdapterParser());
		this.registerBeanDefinitionParser("udp-outbound-channel-adapter", new UdpOutboundChannelAdapterParser());
		this.registerBeanDefinitionParser("tcp-inbound-gateway", new TcpInboundGatewayParser());
		this.registerBeanDefinitionParser("tcp-outbound-gateway", new TcpOutboundGatewayParser());
		this.registerBeanDefinitionParser("tcp-connection-factory", new TcpConnectionFactoryParser());
		this.registerBeanDefinitionParser("tcp-inbound-channel-adapter", new TcpInboundChannelAdapterParser());
		this.registerBeanDefinitionParser("tcp-outbound-channel-adapter", new TcpOutboundChannelAdapterParser());
	}

}
