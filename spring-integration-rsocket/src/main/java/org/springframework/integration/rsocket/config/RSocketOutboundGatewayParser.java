/*
 * Copyright 2019 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
