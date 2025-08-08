/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.hazelcast.config.xml;

import org.springframework.integration.config.xml.AbstractIntegrationNamespaceHandler;

/**
 * Namespace handler for the Hazelcast schema.
 *
 * @author Eren Avsarogullari
 * @since 6.0
 */
public class HazelcastIntegrationNamespaceHandler extends AbstractIntegrationNamespaceHandler {

	@Override
	public void init() {
		registerBeanDefinitionParser("inbound-channel-adapter", new HazelcastEventDrivenInboundChannelAdapterParser());
		registerBeanDefinitionParser("outbound-channel-adapter", new HazelcastOutboundChannelAdapterParser());
		registerBeanDefinitionParser("cq-inbound-channel-adapter", new HazelcastContinuousQueryInboundChannelAdapterParser());
		registerBeanDefinitionParser("ds-inbound-channel-adapter", new HazelcastDistributedSQLInboundChannelAdapterParser());
		registerBeanDefinitionParser("cm-inbound-channel-adapter", new HazelcastClusterMonitorInboundChannelAdapterParser());
	}

}
