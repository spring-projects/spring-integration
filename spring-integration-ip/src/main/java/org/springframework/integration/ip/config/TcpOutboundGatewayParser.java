/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.ip.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractConsumerEndpointParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.ip.tcp.TcpOutboundGateway;

/**
 * Parser for the &lt;outbound-gateway&gt; element of the integration 'jms' namespace.
 *
 * @author Gary Russell
 * @since 2.0
 */
public class TcpOutboundGatewayParser extends AbstractConsumerEndpointParser {

	@Override
	protected String getInputChannelAttributeName() {
		return "request-channel";
	}

	@Override
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(TcpOutboundGateway.class);
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element,
				IpAdapterParserUtils.TCP_CONNECTION_FACTORY);
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element,
				IpAdapterParserUtils.REPLY_CHANNEL);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element,
				IpAdapterParserUtils.REQUEST_TIMEOUT);
		BeanDefinition remoteTimeoutExpression = IntegrationNamespaceUtils
				.createExpressionDefinitionFromValueOrExpression(IpAdapterParserUtils.REMOTE_TIMEOUT,
						IpAdapterParserUtils.REMOTE_TIMEOUT_EXPRESSION, parserContext, element, false);
		if (remoteTimeoutExpression != null) {
			builder.addPropertyValue("remoteTimeoutExpression", remoteTimeoutExpression);
		}
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element,
				IpAdapterParserUtils.REPLY_TIMEOUT, "sendTimeout");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "close-stream-after-send");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "async");
		return builder;
	}

}
