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
 * @author Artem Bilan
 * @since 2.0
 */
public class UdpInboundChannelAdapterParser extends AbstractChannelAdapterParser {

	@Override
	protected AbstractBeanDefinition doParse(Element element, ParserContext parserContext, String channelName) {
		BeanDefinitionBuilder builder = parseUdp(element, parserContext);
		IpAdapterParserUtils.addCommonSocketOptions(builder, element);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element,
				IpAdapterParserUtils.RECEIVE_BUFFER_SIZE);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element,
				IpAdapterParserUtils.POOL_SIZE);
		builder.addPropertyReference("outputChannel", channelName);
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder,
				element, "error-channel", "errorChannel");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element,
				IpAdapterParserUtils.TASK_EXECUTOR);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element,
				IpAdapterParserUtils.LOOKUP_HOST);
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element,
				IpAdapterParserUtils.UDP_SOCKET_CUSTOMIZER);
		return builder.getBeanDefinition();
	}

	private void addPortToConstructor(Element element,
			BeanDefinitionBuilder builder, ParserContext parserContext) {
		String port = IpAdapterParserUtils.getPort(element, parserContext);
		builder.addConstructorArgValue(port);
	}

	private BeanDefinitionBuilder parseUdp(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder;
		String multicast = IpAdapterParserUtils.getMulticast(element);
		if (multicast.equals("false")) {
			builder = BeanDefinitionBuilder.genericBeanDefinition(UnicastReceivingChannelAdapter.class);
		}
		else {
			builder = BeanDefinitionBuilder.genericBeanDefinition(MulticastReceivingChannelAdapter.class);
			String mcAddress = element
					.getAttribute(IpAdapterParserUtils.MULTICAST_ADDRESS);
			if (!StringUtils.hasText(mcAddress)) {
				parserContext.getReaderContext().error(
						IpAdapterParserUtils.MULTICAST_ADDRESS
							+ " is required for a multicast UDP/IP channel adapter",
							element);
			}
			builder.addConstructorArgValue(mcAddress);
		}
		addPortToConstructor(element, builder, parserContext);
		IpAdapterParserUtils.addConstructorValueIfAttributeDefined(builder, element, IpAdapterParserUtils.CHECK_LENGTH);
		return builder;
	}

}
