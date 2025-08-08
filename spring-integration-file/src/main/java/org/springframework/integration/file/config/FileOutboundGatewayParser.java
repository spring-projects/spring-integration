/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.file.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractConsumerEndpointParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;

/**
 * Parser for the 'outbound-gateway' element of the file namespace.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @author Gunnar Hillert
 *
 * @since 1.0.3
 */
public class FileOutboundGatewayParser extends AbstractConsumerEndpointParser {

	@Override
	protected String getInputChannelAttributeName() {
		return "request-channel";
	}

	@Override
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder handlerBuilder = FileWritingMessageHandlerBeanDefinitionBuilder.configure(element, true, parserContext);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(handlerBuilder, element, "reply-timeout", "sendTimeout");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(handlerBuilder, element, "requires-reply");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(handlerBuilder, element, "reply-channel", "outputChannel");
		return handlerBuilder;
	}

}
