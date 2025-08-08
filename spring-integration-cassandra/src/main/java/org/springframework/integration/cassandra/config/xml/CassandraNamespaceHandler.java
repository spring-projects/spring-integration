/*
 * Copyright © 2022 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2022-present the original author or authors.
 */

package org.springframework.integration.cassandra.config.xml;

import org.springframework.integration.config.xml.AbstractIntegrationNamespaceHandler;

/**
 * The namespace handler for "int-cassandra" namespace.
 *
 * @author Artem Bilan
 * @author Filippo Balicchia
 *
 * @since 6.0
 */
public class CassandraNamespaceHandler extends AbstractIntegrationNamespaceHandler {

	@Override
	public void init() {
		registerBeanDefinitionParser("outbound-channel-adapter", new CassandraOutboundChannelAdapterParser());
		registerBeanDefinitionParser("outbound-gateway", new CassandraOutboundGatewayParser());
	}

}
