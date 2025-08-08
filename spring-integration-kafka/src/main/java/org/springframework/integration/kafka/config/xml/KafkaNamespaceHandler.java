/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.kafka.config.xml;

import org.springframework.integration.config.xml.AbstractIntegrationNamespaceHandler;

/**
 * The namespace handler for the Apache Kafka namespace.
 *
 * @author Soby Chacko
 * @author Gary Russell
 *
 * @since 5.4
 *
 */
public class KafkaNamespaceHandler extends AbstractIntegrationNamespaceHandler {

	@Override
	public void init() {
		registerBeanDefinitionParser("outbound-channel-adapter", new KafkaOutboundChannelAdapterParser());
		registerBeanDefinitionParser("message-driven-channel-adapter", new KafkaMessageDrivenChannelAdapterParser());
		registerBeanDefinitionParser("outbound-gateway", new KafkaOutboundGatewayParser());
		registerBeanDefinitionParser("inbound-gateway", new KafkaInboundGatewayParser());
		registerBeanDefinitionParser("inbound-channel-adapter", new KafkaInboundChannelAdapterParser());
		KafkaChannelParser channelParser = new KafkaChannelParser();
		registerBeanDefinitionParser("channel", channelParser);
		registerBeanDefinitionParser("pollable-channel", channelParser);
		registerBeanDefinitionParser("publish-subscribe-channel", channelParser);
	}

}
