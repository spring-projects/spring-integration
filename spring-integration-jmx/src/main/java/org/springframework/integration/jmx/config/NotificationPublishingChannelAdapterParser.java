/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jmx.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractOutboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.jmx.NotificationPublishingMessageHandler;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @since 2.0
 */
public class NotificationPublishingChannelAdapterParser extends AbstractOutboundChannelAdapterParser {

	@Override
	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

	@Override
	protected AbstractBeanDefinition parseConsumer(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder
				.rootBeanDefinition(NotificationPublishingMessageHandler.class);
		builder.addConstructorArgValue(element.getAttribute("object-name"));
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "default-notification-type");
		return builder.getBeanDefinition();
	}

}
