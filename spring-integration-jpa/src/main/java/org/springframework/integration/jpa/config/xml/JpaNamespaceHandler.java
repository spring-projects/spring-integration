/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jpa.config.xml;

import org.springframework.integration.config.xml.AbstractIntegrationNamespaceHandler;

/**
 * The namespace handler for the JPA namespace.
 *
 * @author Amol Nayak
 * @author Gunnar Hillert
 * @since 2.2
 *
 */
public class JpaNamespaceHandler extends AbstractIntegrationNamespaceHandler {

	/* (non-Javadoc)
	 * @see org.springframework.beans.factory.xml.NamespaceHandler#init()
	 */
	public void init() {
		this.registerBeanDefinitionParser("inbound-channel-adapter", new JpaInboundChannelAdapterParser());
		this.registerBeanDefinitionParser("outbound-channel-adapter", new JpaOutboundChannelAdapterParser());
		this.registerBeanDefinitionParser("updating-outbound-gateway", new UpdatingJpaOutboundGatewayParser());
		this.registerBeanDefinitionParser("retrieving-outbound-gateway", new RetrievingJpaOutboundGatewayParser());
	}

}
