/*
 * Copyright 2002-2012 the original author or authors.
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

package org.springframework.integration.ip.config;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractConsumerEndpointParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.w3c.dom.Element;

/**
 * Parser for the &lt;outbound-gateway&gt; element of the integration 'jms' namespace.
 *
 * @author Gary Russell
 * @since 2.0
 */
public class TcpOutboundGatewayParser extends AbstractConsumerEndpointParser {

	private static final String BASE_PACKAGE = "org.springframework.integration.ip.tcp";

	@Override
	protected String getInputChannelAttributeName() {
		return "request-channel";
	}

	@Override
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(BASE_PACKAGE +
				".TcpOutboundGateway");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element,
				IpAdapterParserUtils.TCP_CONNECTION_FACTORY);
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element,
				IpAdapterParserUtils.REPLY_CHANNEL);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element,
				IpAdapterParserUtils.REQUEST_TIMEOUT);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element,
				IpAdapterParserUtils.REMOTE_TIMEOUT);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element,
				IpAdapterParserUtils.REPLY_TIMEOUT, "sendTimeout");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element,
				IpAdapterParserUtils.AUTO_STARTUP);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element,
				IpAdapterParserUtils.PHASE);
		return builder;
	}

}
