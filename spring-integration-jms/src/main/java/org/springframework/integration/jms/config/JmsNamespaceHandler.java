/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jms.config;

import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.integration.config.xml.AbstractIntegrationNamespaceHandler;

/**
 * Namespace handler for Spring Integration's <em>jms</em> namespace.
 *
 * @author Mark Fisher
 */
public class JmsNamespaceHandler extends AbstractIntegrationNamespaceHandler {

	public void init() {
		this.registerBeanDefinitionParser("inbound-gateway", new JmsMessageDrivenEndpointParser(true));
		this.registerBeanDefinitionParser("message-driven-channel-adapter", new JmsMessageDrivenEndpointParser(false));
		this.registerBeanDefinitionParser("inbound-channel-adapter", new JmsInboundChannelAdapterParser());
		this.registerBeanDefinitionParser("outbound-gateway", new JmsOutboundGatewayParser());
		this.registerBeanDefinitionParser("outbound-channel-adapter", new JmsOutboundChannelAdapterParser());
		BeanDefinitionParser channelParser = new JmsChannelParser();
		this.registerBeanDefinitionParser("channel", channelParser);
		this.registerBeanDefinitionParser("publish-subscribe-channel", channelParser);
		this.registerBeanDefinitionParser("header-enricher", new JmsHeaderEnricherParser());
	}

}
