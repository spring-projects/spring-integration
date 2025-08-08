/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.redis.config;

import org.springframework.integration.config.xml.AbstractIntegrationNamespaceHandler;

/**
 *  Namespace handler for Spring Integration's 'redis' namespace.
 *
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @since 2.1
 */
public class RedisNamespaceHandler extends AbstractIntegrationNamespaceHandler {

	public void init() {
		registerBeanDefinitionParser("publish-subscribe-channel", new RedisChannelParser());
		registerBeanDefinitionParser("inbound-channel-adapter", new RedisInboundChannelAdapterParser());
		registerBeanDefinitionParser("store-inbound-channel-adapter", new RedisStoreInboundChannelAdapterParser());
		registerBeanDefinitionParser("store-outbound-channel-adapter", new RedisStoreOutboundChannelAdapterParser());
		registerBeanDefinitionParser("outbound-channel-adapter", new RedisOutboundChannelAdapterParser());
		registerBeanDefinitionParser("queue-inbound-channel-adapter", new RedisQueueInboundChannelAdapterParser());
		registerBeanDefinitionParser("queue-outbound-channel-adapter", new RedisQueueOutboundChannelAdapterParser());
		registerBeanDefinitionParser("outbound-gateway", new RedisOutboundGatewayParser());
		registerBeanDefinitionParser("queue-inbound-gateway", new RedisQueueInboundGatewayParser());
		registerBeanDefinitionParser("queue-outbound-gateway", new RedisQueueOutboundGatewayParser());
	}

}
