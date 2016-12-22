/*
 * Copyright 2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.mongodb.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractConsumerEndpointParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.mongodb.outbound.MongoDbOutboundGateway;
import org.springframework.util.StringUtils;

/**
 * Parser for MongoDb outbound gateways
 *
 * @author Xavier Padró
 * @since 5.0
 */
public class MongoDbOutboundGatewayParser extends AbstractConsumerEndpointParser {

	@Override
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
		final BeanDefinitionBuilder builder =
				BeanDefinitionBuilder.genericBeanDefinition(MongoDbOutboundGateway.class);

		MongoParserUtils.processCommonAttributes(element, parserContext, builder);

		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "reply-timeout");
		String replyChannel = element.getAttribute("reply-channel");

		if (StringUtils.hasText(replyChannel)) {
			builder.addPropertyReference("outputChannel", replyChannel);
		}

		BeanDefinition queryExpressionDef =
				IntegrationNamespaceUtils.createExpressionDefinitionFromValueOrExpression("query", "query-expression",
						parserContext, element, true);

		if (queryExpressionDef != null) {
			builder.addPropertyValue("queryExpression", queryExpressionDef);
		}

		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "expect-single-result");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "entity-class");

		return builder;
	}

	@Override
	protected String getInputChannelAttributeName() {
		return "request-channel";
	}
}
