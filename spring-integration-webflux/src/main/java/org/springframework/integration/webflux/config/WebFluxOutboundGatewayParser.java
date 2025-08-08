/*
 * Copyright © 2017 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2017-present the original author or authors.
 */

package org.springframework.integration.webflux.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.http.config.HttpOutboundGatewayParser;

/**
 * Parser for the 'outbound-gateway' element of the webflux namespace.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
public class WebFluxOutboundGatewayParser extends HttpOutboundGatewayParser {

	@Override
	protected BeanDefinitionBuilder getBuilder(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder =
				WebFluxOutboundChannelAdapterParser.buildWebFluxRequestExecutingMessageHandler(element, parserContext);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "reply-payload-to-flux");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "body-extractor");
		return builder;
	}

}
