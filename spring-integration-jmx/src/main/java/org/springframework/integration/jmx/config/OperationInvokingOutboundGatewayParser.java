/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.jmx.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractConsumerEndpointParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.jmx.OperationInvokingMessageHandler;

/**
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class OperationInvokingOutboundGatewayParser extends AbstractConsumerEndpointParser {

	@Override
	protected String getInputChannelAttributeName() {
		return "request-channel";
	}

	@Override
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(OperationInvokingMessageHandler.class);
		builder.addConstructorArgReference(element.getAttribute("server"));
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "object-name");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "operation-name");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "reply-channel", "outputChannel");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "requires-reply");
		return builder;
	}

}
