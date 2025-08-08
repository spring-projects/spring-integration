/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jmx.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.jmx.NotificationListeningMessageProducer;

/**
 * @author Mark Fisher
 * @author Gary Russell
 * @since 2.0
 */
public class NotificationListeningChannelAdapterParser extends AbstractChannelAdapterParser {

	@Override
	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

	@Override
	protected AbstractBeanDefinition doParse(Element element,
			ParserContext parserContext, String channelName) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(NotificationListeningMessageProducer.class);
		builder.addPropertyReference("outputChannel", channelName);
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "server");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "notification-filter", "filter");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "handback");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "send-timeout");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "object-name");
		return builder.getBeanDefinition();
	}

}
