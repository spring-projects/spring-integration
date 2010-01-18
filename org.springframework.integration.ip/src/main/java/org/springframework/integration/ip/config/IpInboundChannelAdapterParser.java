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

import org.w3c.dom.Element;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.ip.udp.MulticastReceivingChannelAdapter;
import org.springframework.integration.ip.udp.UnicastReceivingChannelAdapter;
import org.springframework.util.StringUtils;

/**
 * Channel Adapter that receives UDP datagram packets and maps them to Messages.
 * 
 * @author Gary Russell
 * @since 2.0
 */
public class IpInboundChannelAdapterParser extends AbstractChannelAdapterParser {

	protected AbstractBeanDefinition doParse(Element element, ParserContext parserContext, String channelName) {
		String protocol = IpAdapterParserUtils.getProtocol(element);
		String multicast = IpAdapterParserUtils.getMulticast(element);
		BeanDefinitionBuilder builder = null;
		if (protocol.equals("tcp")) {
			throw new BeanCreationException("tcp not yet supported");
		}
		else if (protocol.equals("udp")) {
			if (multicast.equals("false")) {
				builder = BeanDefinitionBuilder
						.genericBeanDefinition(UnicastReceivingChannelAdapter.class);
			}
			else {
				builder = BeanDefinitionBuilder
						.genericBeanDefinition(MulticastReceivingChannelAdapter.class);
				String mcAddress = element
						.getAttribute(IpAdapterParserUtils.MULTICAST_ADDRESS);
				if (!StringUtils.hasText(mcAddress)) {
					throw new BeanCreationException(
							IpAdapterParserUtils.MULTICAST_ADDRESS
									+ " is required for a multicast UDP/IP channel adapter");
				}
				builder.addConstructorArgValue(mcAddress);
			}
		}
		String port = IpAdapterParserUtils.getPort(element);
		builder.addConstructorArgValue(port);
		IpAdapterParserUtils.addConstuctirValueIfAttributeDefined(builder,
				element, IpAdapterParserUtils.CHECK_LENGTH, true);
		IpAdapterParserUtils.addCommonSocketOptions(builder, element);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element,
				IpAdapterParserUtils.RECEIVE_BUFFER_SIZE);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element,
				IpAdapterParserUtils.POOL_SIZE);
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder,
				element, "channel", "outputChannel");
		return builder.getBeanDefinition();
	}

}
