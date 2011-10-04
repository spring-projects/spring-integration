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

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.w3c.dom.Element;

/**
 * Channel Adapter that receives UDP datagram packets and maps them to Messages.
 * 
 * @author Gary Russell
 * @since 2.0
 */
public class TcpInboundChannelAdapterParser extends AbstractChannelAdapterParser {
	
	private static final String BASE_PACKAGE = "org.springframework.integration.ip.tcp";

	protected AbstractBeanDefinition doParse(Element element, ParserContext parserContext, String channelName) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(BASE_PACKAGE +
				".TcpReceivingChannelAdapter");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, 
				IpAdapterParserUtils.TCP_CONNECTION_FACTORY);
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder,
				element, "channel", "outputChannel");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder,
				element, "error-channel", "errorChannel");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element,
				IpAdapterParserUtils.AUTO_STARTUP);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element,
				IpAdapterParserUtils.PHASE);
		return builder.getBeanDefinition();
	}

}
