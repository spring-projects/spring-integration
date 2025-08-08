/*
 * Copyright © 2019 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2019-present the original author or authors.
 */

package org.springframework.integration.rsocket.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.core.Conventions;
import org.springframework.integration.config.xml.AbstractConsumerEndpointParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.rsocket.outbound.RSocketOutboundGateway;

/**
 * Parser for the 'outbound-gateway' element of the rsocket namespace.
 *
 * @author Artem Bilan
 *
 * @since 5.2
 */
public class RSocketOutboundGatewayParser extends AbstractConsumerEndpointParser {

	@Override
	protected String getInputChannelAttributeName() {
		return "request-channel";
	}

	@Override
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(RSocketOutboundGateway.class);
		BeanDefinition routeExpression =
				IntegrationNamespaceUtils.createExpressionDefinitionFromValueOrExpression("route", "route-expression",
						parserContext, element, true);
		builder.addConstructorArgValue(routeExpression);
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "client-rsocket-connector",
				"clientRSocketConnector");
		populateValueOrExpressionIfAny(builder, element, parserContext, "interaction-model");
		populateValueOrExpressionIfAny(builder, element, parserContext, "command");
		populateValueOrExpressionIfAny(builder, element, parserContext, "publisher-element-type");
		populateValueOrExpressionIfAny(builder, element, parserContext, "expected-response-type");
		populateValueOrExpressionIfAny(builder, element, parserContext, "metadata");
		return builder;
	}

	private static void populateValueOrExpressionIfAny(BeanDefinitionBuilder builder, Element element,
			ParserContext parserContext, String valueAttributeName) {

		String expressionAttributeName = valueAttributeName + "-expression";

		BeanDefinition expression =
				IntegrationNamespaceUtils.createExpressionDefinitionFromValueOrExpression(valueAttributeName,
						expressionAttributeName, parserContext, element, false);
		if (expression != null) {
			builder.addPropertyValue(Conventions.attributeNameToPropertyName(expressionAttributeName), expression);
		}
	}

}
