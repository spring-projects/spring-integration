/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.amqp.config;

import org.springframework.integration.config.xml.AbstractIntegrationNamespaceHandler;

/**
 * Namespace handler for the AMQP schema.
 *
 * @author Mark Fisher
 * @author Gary Russell
 *
 * @since 2.1
 */
public class AmqpNamespaceHandler extends AbstractIntegrationNamespaceHandler {

	@Override
	public void init() {
		registerBeanDefinitionParser("channel", new AmqpChannelParser());
		registerBeanDefinitionParser("publish-subscribe-channel", new AmqpChannelParser());
		registerBeanDefinitionParser("inbound-channel-adapter", new AmqpInboundChannelAdapterParser());
		registerBeanDefinitionParser("inbound-gateway", new AmqpInboundGatewayParser());
		registerBeanDefinitionParser("outbound-channel-adapter", new AmqpOutboundChannelAdapterParser());
		registerBeanDefinitionParser("outbound-gateway", new AmqpOutboundGatewayParser());
		registerBeanDefinitionParser("outbound-async-gateway", new AmqpOutboundGatewayParser());
	}

}
