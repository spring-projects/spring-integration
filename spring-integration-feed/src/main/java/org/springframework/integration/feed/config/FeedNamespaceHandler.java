/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.feed.config;

import org.springframework.integration.config.xml.AbstractIntegrationNamespaceHandler;

/**
 * NamespaceHandler for the feed module.
 *
 * @author Josh Long
 * @author Mark Fisher
 * @since 2.0
 */
public class FeedNamespaceHandler extends AbstractIntegrationNamespaceHandler {

	public void init() {
		registerBeanDefinitionParser("inbound-channel-adapter", new FeedInboundChannelAdapterParser());
	}

}
