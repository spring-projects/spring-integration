/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.event.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractOutboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.event.outbound.ApplicationEventPublishingMessageHandler;

/**
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @since 2.0
 */
public class EventOutboundChannelAdapterParser extends AbstractOutboundChannelAdapterParser {

	@Override
	protected AbstractBeanDefinition parseConsumer(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder =
				BeanDefinitionBuilder.genericBeanDefinition(ApplicationEventPublishingMessageHandler.class);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "publish-payload");
		return builder.getBeanDefinition();
	}

}
