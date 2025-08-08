/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.xmpp.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;

/**
 * Parser for the XMPP 'inbound-channel-adapter' element.
 *
 * @author Josh Long
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @since 2.0
 */
public class ChatMessageInboundChannelAdapterParser extends AbstractXmppInboundChannelAdapterParser {

	@Override
	protected String getBeanClassName(Element element) {
		return "org.springframework.integration.xmpp.inbound.ChatMessageListeningEndpoint";
	}

	@Override
	protected void postProcess(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		BeanDefinition expression =
				IntegrationNamespaceUtils.createExpressionDefIfAttributeDefined("payload-expression", element);
		if (expression != null) {
			builder.addPropertyValue("payloadExpression", expression);
		}
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "stanza-filter");
	}

}
