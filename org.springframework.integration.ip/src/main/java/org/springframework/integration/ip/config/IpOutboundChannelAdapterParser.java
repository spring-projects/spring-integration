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
import org.springframework.integration.config.xml.AbstractOutboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.ip.udp.MulticastSendingMessageHandler;
import org.springframework.integration.ip.udp.UnicastSendingMessageHandler;
import org.springframework.util.StringUtils;

/**
 * @author Gary Russell
 * @since 2.0
 */
public class IpOutboundChannelAdapterParser extends AbstractOutboundChannelAdapterParser {

	protected AbstractBeanDefinition parseConsumer(Element element, ParserContext parserContext) {
		String protocol = IpAdapterParserUtils.getProtocol(element);
		BeanDefinitionBuilder builder = null;
		if (protocol.equals("tcp")) {
			throw new BeanCreationException("tcp not yet supported");
		}
		else if (protocol.equals("udp")) {
			String multicast = IpAdapterParserUtils.getMulticast(element);
			if (multicast.equals("true")) {
				builder = BeanDefinitionBuilder
						.genericBeanDefinition(MulticastSendingMessageHandler.class);
				IntegrationNamespaceUtils.setValueIfAttributeDefined(builder,
						element, IpAdapterParserUtils.MIN_ACKS_SUCCESS,
						"minAcksForSuccess");
				IntegrationNamespaceUtils.setValueIfAttributeDefined(builder,
						element, IpAdapterParserUtils.TIME_TO_LIVE,
						"timeToLive");
			}
			else {
				builder = BeanDefinitionBuilder
						.genericBeanDefinition(UnicastSendingMessageHandler.class);
			}
		}
		String host = element.getAttribute(IpAdapterParserUtils.HOST);
		if (!StringUtils.hasText(host)) {
			throw new BeanCreationException(IpAdapterParserUtils.HOST
					+ " is required for IP outbound channel adapters");
		}
		builder.addConstructorArgValue(host);
		String port = IpAdapterParserUtils.getPort(element);
		builder.addConstructorArgValue(port);
		IpAdapterParserUtils.addConstuctirValueIfAttributeDefined(builder,
				element, IpAdapterParserUtils.CHECK_LENGTH, true);
		IpAdapterParserUtils.addConstuctirValueIfAttributeDefined(builder,
				element, IpAdapterParserUtils.ACK, true);
		IpAdapterParserUtils.addConstuctirValueIfAttributeDefined(builder,
				element, IpAdapterParserUtils.ACK_HOST, false);
		IpAdapterParserUtils.addConstuctirValueIfAttributeDefined(builder,
				element, IpAdapterParserUtils.ACK_PORT, false);
		IpAdapterParserUtils.addConstuctirValueIfAttributeDefined(builder,
				element, IpAdapterParserUtils.ACK_TIMEOUT, false);
		String ack = element.getAttribute(IpAdapterParserUtils.ACK);
		if (ack.equals("true")) {
			if (!StringUtils.hasText(element
					.getAttribute(IpAdapterParserUtils.ACK_HOST))
					|| !StringUtils.hasText(element
							.getAttribute(IpAdapterParserUtils.ACK_PORT))
					|| !StringUtils.hasText(element
							.getAttribute(IpAdapterParserUtils.ACK_TIMEOUT))) {
				throw new BeanCreationException("When "
						+ IpAdapterParserUtils.ACK + " is true, "
						+ IpAdapterParserUtils.ACK_HOST + ", "
						+ IpAdapterParserUtils.ACK_PORT + ", and "
						+ IpAdapterParserUtils.ACK_TIMEOUT
						+ " must be supplied");
			}
		}
		IpAdapterParserUtils.addCommonSocketOptions(builder, element);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element,
				IpAdapterParserUtils.RECEIVE_BUFFER_SIZE);
		return builder.getBeanDefinition();
	}

}
