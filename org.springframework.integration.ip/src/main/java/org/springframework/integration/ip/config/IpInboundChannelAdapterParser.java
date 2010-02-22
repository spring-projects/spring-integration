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
import org.springframework.core.Conventions;
import org.springframework.integration.config.xml.AbstractChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.ip.tcp.TcpNetReceivingChannelAdapter;
import org.springframework.integration.ip.tcp.TcpNioReceivingChannelAdapter;
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
		BeanDefinitionBuilder builder = null;
		if (protocol.equals("tcp")) {
			builder = parseTcp(element);
		} else if (protocol.equals("udp")) {
			builder = parseUdp(element);
		}
		IpAdapterParserUtils.addCommonSocketOptions(builder, element);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element,
				IpAdapterParserUtils.RECEIVE_BUFFER_SIZE);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element,
				IpAdapterParserUtils.POOL_SIZE);
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder,
				element, "channel", "outputChannel");
		return builder.getBeanDefinition();
	}

	/**
	 * @param element
	 * @param builder
	 */
	private void addPortToConstructor(Element element,
			BeanDefinitionBuilder builder) {
		String port = IpAdapterParserUtils.getPort(element);
		builder.addConstructorArgValue(port);
	}

	/**
	 * @param element
	 * @return
	 */
	private BeanDefinitionBuilder parseUdp(Element element) {
		BeanDefinitionBuilder builder;
		String multicast = IpAdapterParserUtils.getMulticast(element);
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
		addPortToConstructor(element, builder);
		IpAdapterParserUtils.addConstuctorValueIfAttributeDefined(builder,
				element, IpAdapterParserUtils.CHECK_LENGTH, true);
		return builder;
	}

	/**
	 * @param element
	 * @return
	 */
	private BeanDefinitionBuilder parseTcp(Element element) {
		BeanDefinitionBuilder builder;
		String useNio = IpAdapterParserUtils.getUseNio(element);
		if (useNio.equals("false")) {
			builder = BeanDefinitionBuilder
					.genericBeanDefinition(TcpNetReceivingChannelAdapter.class);
		}
		else {
			builder = BeanDefinitionBuilder
					.genericBeanDefinition(TcpNioReceivingChannelAdapter.class);
		}
		addPortToConstructor(element, builder);
		builder.addPropertyValue(
				Conventions.attributeNameToPropertyName(IpAdapterParserUtils.MESSAGE_FORMAT), 
				IpAdapterParserUtils.getMessageFormat(element));
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, 
				IpAdapterParserUtils.CUSTOM_SOCKET_READER_CLASS_NAME); 
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, 
				IpAdapterParserUtils.USING_DIRECT_BUFFERS);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, 
				IpAdapterParserUtils.SO_KEEP_ALIVE);
		return builder;
	}

}
