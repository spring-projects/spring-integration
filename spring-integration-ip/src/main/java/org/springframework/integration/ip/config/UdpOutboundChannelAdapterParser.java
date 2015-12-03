/*
 * Copyright 2002-2013 the original author or authors.
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
import org.springframework.integration.config.xml.AbstractOutboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.ip.udp.UnicastDatagramSocketRegistry;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * @author Gary Russell
 * @since 2.0
 */
public class UdpOutboundChannelAdapterParser extends AbstractOutboundChannelAdapterParser {

	private static final String BASE_PACKAGE = "org.springframework.integration.ip.udp";

	protected AbstractBeanDefinition parseConsumer(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = parseUdp(element, parserContext);
		IpAdapterParserUtils.addCommonSocketOptions(builder, element);
		return builder.getBeanDefinition();
	}

	private BeanDefinitionBuilder parseUdp(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder;
		String multicast = IpAdapterParserUtils.getMulticast(element);
		if (multicast.equals("true")) {
			builder = BeanDefinitionBuilder.genericBeanDefinition(BASE_PACKAGE +
					".MulticastSendingMessageHandler");
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder,
					element, IpAdapterParserUtils.MIN_ACKS_SUCCESS,
					"minAcksForSuccess");
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder,
					element, IpAdapterParserUtils.TIME_TO_LIVE,
					"timeToLive");
		}
		else {
			builder = BeanDefinitionBuilder.genericBeanDefinition(BASE_PACKAGE +
					".UnicastSendingMessageHandler");
		}
		IpAdapterParserUtils.addHostAndPortToConstructor(element, builder, parserContext);
		if (multicast.equals("false")) {
			builder.addConstructorArgValue(UnicastDatagramSocketRegistry.INSTANCE);
		}
		IpAdapterParserUtils.addConstuctorValueIfAttributeDefined(builder,
				element, IpAdapterParserUtils.CHECK_LENGTH, true);
		IpAdapterParserUtils.addConstuctorValueIfAttributeDefined(builder,
				element, IpAdapterParserUtils.ACK, true);
		IpAdapterParserUtils.addConstuctorValueIfAttributeDefined(builder,
				element, IpAdapterParserUtils.ACK_HOST, false);
		IpAdapterParserUtils.addConstuctorValueIfAttributeDefined(builder,
				element, IpAdapterParserUtils.ACK_PORT, false);
		IpAdapterParserUtils.addConstuctorValueIfAttributeDefined(builder,
				element, IpAdapterParserUtils.ACK_TIMEOUT, false);
		String ack = element.getAttribute(IpAdapterParserUtils.ACK);
		if (ack.equals("true")) {
			if (!StringUtils.hasText(element
					.getAttribute(IpAdapterParserUtils.ACK_HOST))
					|| !StringUtils.hasText(element
							.getAttribute(IpAdapterParserUtils.ACK_PORT))
					|| !StringUtils.hasText(element
							.getAttribute(IpAdapterParserUtils.ACK_TIMEOUT))) {
				parserContext.getReaderContext().error("When "
						+ IpAdapterParserUtils.ACK + " is true, "
						+ IpAdapterParserUtils.ACK_HOST + ", "
						+ IpAdapterParserUtils.ACK_PORT + ", and "
						+ IpAdapterParserUtils.ACK_TIMEOUT
						+ " must be supplied", element);
			}
		}
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element,
				IpAdapterParserUtils.RECEIVE_BUFFER_SIZE);
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element,
				IpAdapterParserUtils.TASK_EXECUTOR);
		return builder;
	}

}
