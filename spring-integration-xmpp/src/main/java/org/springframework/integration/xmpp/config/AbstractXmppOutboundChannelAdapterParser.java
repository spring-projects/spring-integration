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
import org.springframework.integration.config.xml.AbstractOutboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.xmpp.support.DefaultXmppHeaderMapper;
import org.springframework.util.StringUtils;

/**
 * Base class of XMPP outbound parsers
 *
 * @author Oleg Zhurakousky
 * @since 2.0.1
 */
public abstract class AbstractXmppOutboundChannelAdapterParser extends AbstractOutboundChannelAdapterParser {

	@Override
	protected AbstractBeanDefinition parseConsumer(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(this.getHandlerClassName());

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

		return builder.getBeanDefinition();
	}

	protected abstract String getHandlerClassName();

}
