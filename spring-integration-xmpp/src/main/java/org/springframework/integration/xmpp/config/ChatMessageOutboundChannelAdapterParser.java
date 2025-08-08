/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.xmpp.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.xmpp.outbound.ChatMessageSendingMessageHandler;
import org.springframework.util.StringUtils;

/**
 * Parser for the XMPP 'outbound-channel-adapter' element
 *
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class ChatMessageOutboundChannelAdapterParser extends AbstractXmppOutboundChannelAdapterParser {

	@Override
	protected String getHandlerClassName() {
		return ChatMessageSendingMessageHandler.class.getName();
	}

	@Override
	protected AbstractBeanDefinition parseConsumer(Element element, ParserContext parserContext) {
		AbstractBeanDefinition beanDefinition = super.parseConsumer(element, parserContext);
		String extensionProvider = element.getAttribute("extension-provider");
		if (StringUtils.hasText(extensionProvider)) {
			beanDefinition.getPropertyValues()
					.addPropertyValue("extensionProvider", new RuntimeBeanReference(extensionProvider));
		}
		return beanDefinition;
	}

}
