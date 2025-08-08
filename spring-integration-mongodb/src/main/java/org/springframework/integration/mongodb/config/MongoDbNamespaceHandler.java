/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.mongodb.config;

import org.springframework.integration.config.xml.AbstractIntegrationNamespaceHandler;

/**
 *  Namespace handler for Spring Integration's 'mongodb' namespace.
 *
 * @author Amol Nayak
 * @author Oleg Zhurakousky
 *
 * @since 2.2
 */
public class MongoDbNamespaceHandler extends AbstractIntegrationNamespaceHandler {

	public void init() {
		registerBeanDefinitionParser("inbound-channel-adapter", new MongoDbInboundChannelAdapterParser());
		registerBeanDefinitionParser("outbound-channel-adapter", new MongoDbOutboundChannelAdapterParser());
		registerBeanDefinitionParser("outbound-gateway", new MongoDbOutboundGatewayParser());
	}

}
