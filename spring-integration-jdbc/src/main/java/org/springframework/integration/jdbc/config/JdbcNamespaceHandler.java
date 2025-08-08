/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jdbc.config;

import org.springframework.integration.config.xml.AbstractIntegrationNamespaceHandler;

/**
 * Namespace handler for the integration JDBC schema.
 *
 * @author Jonas Partner
 * @author Dave Syer
 * @author Gunnar Hillert
 *
 * @since 2.0
 */
public class JdbcNamespaceHandler extends AbstractIntegrationNamespaceHandler {

	public void init() {
		registerBeanDefinitionParser("inbound-channel-adapter", new JdbcPollingChannelAdapterParser());
		registerBeanDefinitionParser("outbound-channel-adapter", new JdbcMessageHandlerParser());
		registerBeanDefinitionParser("outbound-gateway", new JdbcOutboundGatewayParser());
		registerBeanDefinitionParser("message-store", new JdbcMessageStoreParser());
		registerBeanDefinitionParser("stored-proc-outbound-channel-adapter", new StoredProcMessageHandlerParser());
		registerBeanDefinitionParser("stored-proc-inbound-channel-adapter", new StoredProcPollingChannelAdapterParser());
		registerBeanDefinitionParser("stored-proc-outbound-gateway", new StoredProcOutboundGatewayParser());
	}

}
