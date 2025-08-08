/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.event.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.event.inbound.ApplicationEventListeningMessageProducer;

/**
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 * @since 2.0
 */
public class EventInboundChannelAdapterParser extends AbstractChannelAdapterParser {

	@Override
	protected AbstractBeanDefinition doParse(Element element, ParserContext parserContext, String channelName) {
		BeanDefinitionBuilder adapterBuilder = BeanDefinitionBuilder
				.rootBeanDefinition(ApplicationEventListeningMessageProducer.class);
		adapterBuilder.addPropertyReference("outputChannel", channelName);
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(adapterBuilder, element, "error-channel");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(adapterBuilder, element, "event-types");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(adapterBuilder, element, "payload-expression",
				"payloadExpressionString");
		return adapterBuilder.getBeanDefinition();
	}

}
