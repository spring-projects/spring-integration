/*
 * Copyright 2002-2020 the original author or authors.
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
