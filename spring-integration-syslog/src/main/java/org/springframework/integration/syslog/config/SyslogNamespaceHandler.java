/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.syslog.config;

import org.springframework.integration.config.xml.AbstractIntegrationNamespaceHandler;

/**
 * Namspace handler for spring-integration-syslog.
 * @author Gary Russell
 * @since 3.0
 *
 */
public class SyslogNamespaceHandler extends AbstractIntegrationNamespaceHandler {

	@Override
	public void init() {
		this.registerBeanDefinitionParser("inbound-channel-adapter", new SyslogInboundChannelAdapterParser());
	}

}
