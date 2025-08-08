/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
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
