/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.mqtt.config.xml;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;

/**
 * The MqttAdapter Message Driven Channel adapter parser.
 *
 * @author Gary Russell
 * @author Artem Bilan
 *
 * @since 4.0
 *
 */
public class MqttMessageDrivenChannelAdapterParser extends AbstractChannelAdapterParser {

	@Override
	protected AbstractBeanDefinition doParse(Element element, ParserContext parserContext, String channelName) {

		BeanDefinitionBuilder builder = BeanDefinitionBuilder
				.genericBeanDefinition(MqttPahoMessageDrivenChannelAdapter.class);

		MqttParserUtils.parseCommon(element, builder, parserContext);
		builder.addConstructorArgValue(element.getAttribute("topics"));
		builder.addPropertyReference("outputChannel", channelName);
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "error-channel");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "qos");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "manual-acks");

		return builder.getBeanDefinition();
	}

}
