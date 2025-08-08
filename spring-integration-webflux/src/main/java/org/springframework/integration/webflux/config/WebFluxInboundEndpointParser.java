/*
 * Copyright © 2018 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2018-present the original author or authors.
 */

package org.springframework.integration.webflux.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.http.config.HttpInboundEndpointParser;
import org.springframework.integration.webflux.inbound.WebFluxInboundEndpoint;

/**
 * @author Artem Bilan
 *
 * @since 5.0.1
 */
public class WebFluxInboundEndpointParser extends HttpInboundEndpointParser {

	public WebFluxInboundEndpointParser(boolean expectReply) {
		super(expectReply);
	}

	@Override
	protected Class<?> getBeanClass(Element element) {
		return WebFluxInboundEndpoint.class;
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		super.doParse(element, parserContext, builder);

		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "codec-configurer");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "requested-content-type-resolver");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "reactive-adapter-registry");
	}

}
