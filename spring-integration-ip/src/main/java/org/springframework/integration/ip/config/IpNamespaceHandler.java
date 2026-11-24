/*
 * Copyright and licensing information for this source code can be found in
 * the NOTICE and LICENSE files distributed with this project.
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
