/*
 * Copyright © 2015 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2015-present the original author or authors.
 */

package org.springframework.integration.stomp.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.stomp.inbound.StompInboundChannelAdapter;

/**
 * The {@link AbstractChannelAdapterParser} implementation for
 * the {@code <stomp:inbound-channel-adapter/>} element.
 *
 * @author Artem Bilan
 * @author Gary Russell
 * @since 4.2
 */
public class StompInboundChannelAdapterParser extends AbstractChannelAdapterParser {

	@Override
	protected AbstractBeanDefinition doParse(Element element, ParserContext parserContext, String channelName) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(StompInboundChannelAdapter.class);
		StompAdapterParserUtils.configureStompAdapter(builder, parserContext, element);
		builder.addConstructorArgValue(element.getAttribute("destinations"));
		builder.addPropertyReference("outputChannel", channelName);
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "error-channel");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "send-timeout");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "payload-type");
		return builder.getBeanDefinition();
	}

}
