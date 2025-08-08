/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.ws.config;

import org.springframework.integration.config.xml.AbstractIntegrationNamespaceHandler;

/**
 * @author Mark Fisher
 * @author Iwein Fuld
 */
public class WsNamespaceHandler extends AbstractIntegrationNamespaceHandler {

	public void init() {
		this.registerBeanDefinitionParser("outbound-gateway", new WebServiceOutboundGatewayParser());
		this.registerBeanDefinitionParser("inbound-gateway", new WebServiceInboundGatewayParser());
		this.registerBeanDefinitionParser("header-enricher", new WebServiceHeaderEnricherParser());
	}

}
