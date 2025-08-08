/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.stream.config;

import org.springframework.integration.config.xml.AbstractIntegrationNamespaceHandler;

/**
 * @author Mark Fisher
 */
public class StreamNamespaceHandler extends AbstractIntegrationNamespaceHandler {

	public void init() {
		this.registerBeanDefinitionParser("stdin-channel-adapter", new ConsoleInboundChannelAdapterParser());
		this.registerBeanDefinitionParser("stdout-channel-adapter", new ConsoleOutboundChannelAdapterParser());
		this.registerBeanDefinitionParser("stderr-channel-adapter", new ConsoleOutboundChannelAdapterParser());
	}

}
