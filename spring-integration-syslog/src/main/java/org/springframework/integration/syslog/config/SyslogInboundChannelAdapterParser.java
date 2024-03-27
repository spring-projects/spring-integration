/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.syslog.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.ip.config.UdpInboundChannelAdapterParser;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * Parses a {@code <int-syslog:inbound-channel-adapter/>}.
 * @author Gary Russell
 * @since 3.0
 *
 */
public class SyslogInboundChannelAdapterParser extends UdpInboundChannelAdapterParser {

	@Override
	protected AbstractBeanDefinition doParse(Element element, ParserContext parserContext, String channelName) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(SyslogReceivingChannelAdapterFactoryBean.class);
		String protocol = element.getAttribute("protocol");
		if (!StringUtils.hasText(protocol)) {
			protocol = SyslogReceivingChannelAdapterFactoryBean.Protocol.udp.toString();
		}
		builder.addConstructorArgValue(protocol);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "port");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element,
				"connection-factory");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "converter");
		Element udpAdapterElement = DomUtils.getChildElementByTagName(element, "udp-attributes");
		if (udpAdapterElement != null) {
			if (StringUtils.hasText(element.getAttribute("port"))) {
				parserContext.getReaderContext().error(
						"When child element 'udp-attributes' is present, 'port' must be defined there", element);
			}
			BeanDefinition udpAdapterDef = super.doParse(udpAdapterElement, parserContext, channelName);
			builder.addPropertyValue("udpAdapter", udpAdapterDef);
		}
		builder.addPropertyReference("outputChannel", channelName);
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder,
				element, "error-channel", "errorChannel");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "send-timeout");
		return builder.getBeanDefinition();
	}

}
