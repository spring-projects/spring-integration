/*
 * Copyright 2002-2010 the original author or authors.
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
 */
public class IpOutboundGatewayParser extends AbstractConsumerEndpointParser {

	@Override
	protected String getInputChannelAttributeName() {
		return "request-channel";
	}

	@Override
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(
				"org.springframework.integration.ip.tcp.SimpleTcpNetOutboundGateway");
		IpAdapterParserUtils.addHostAndPortToConstructor(element, builder, parserContext);
		IpAdapterParserUtils.addCommonSocketOptions(builder, element);
		IpAdapterParserUtils.addOutboundTcpAttributes(element, builder);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, 
				IpAdapterParserUtils.CUSTOM_SOCKET_READER_CLASS_NAME); 
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "reply-channel");
		return builder;
	}

}
