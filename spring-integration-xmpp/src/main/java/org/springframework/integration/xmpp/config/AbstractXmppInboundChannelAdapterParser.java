/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.xmpp.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.xmpp.support.DefaultXmppHeaderMapper;
import org.springframework.util.StringUtils;

/**
 * Base class for XMPP inbound parsers
 *
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @since 2.0.1
 */
public abstract class AbstractXmppInboundChannelAdapterParser extends AbstractChannelAdapterParser {

	@Override
	protected boolean shouldGenerateId() {
		return false;
	}

	@Override
	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

	protected abstract String getBeanClassName(Element element);

	@Override
	protected AbstractBeanDefinition doParse(Element element, ParserContext parserContext, String channelName) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(getBeanClassName(element));

		IntegrationNamespaceUtils.configureHeaderMapper(element, builder, parserContext, DefaultXmppHeaderMapper.class, null);

		String connectionName = element.getAttribute("xmpp-connection");

		if (StringUtils.hasText(connectionName)) {
			builder.addConstructorArgReference(connectionName);
		}
		else if (parserContext.getRegistry().containsBeanDefinition(XmppNamespaceHandler.XMPP_CONNECTION_BEAN_NAME)) {
			builder.addConstructorArgReference(XmppNamespaceHandler.XMPP_CONNECTION_BEAN_NAME);
		}
		else {
			throw new BeanCreationException("You must either explicitly define which XMPP connection to use via " +
					"'xmpp-connection' attribute or have default XMPP connection bean registered under the name 'xmppConnection'" +
					"(e.g., <int-xmpp:xmpp-connection .../>). If 'id' is not provided the default will be 'xmppConnection'.");
		}
		builder.addPropertyReference("outputChannel", channelName);
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "error-channel");
		this.postProcess(element, parserContext, builder);
		return builder.getBeanDefinition();
	}

	protected void postProcess(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		// no op
	}

}
