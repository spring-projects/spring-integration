/*
 * Copyright © 2002 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2002-present the original author or authors.
 */

package org.springframework.integration.xmpp.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.util.StringUtils;

/**
 * Parser for 'xmpp:xmpp-connection' element
 *
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 *
 * @since 2.0
 */
public class XmppConnectionParser extends AbstractSingleBeanDefinitionParser {

	@Override
	protected Class<?> getBeanClass(Element element) {
		return XmppConnectionFactoryBean.class;
	}

	@Override
	protected boolean shouldGenerateIdAsFallback() {
		return false;
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String serviceName = element.getAttribute("service-name");
		String user = element.getAttribute("user");

		if (!StringUtils.hasText(serviceName) && !StringUtils.hasText(user)) {
			parserContext.getReaderContext().error("One of 'service-name' or 'user' attributes is required", element);
		}

		String[] attributes = {"user", "password", "resource", "host", "port", "service-name",
				IntegrationNamespaceUtils.AUTO_STARTUP, IntegrationNamespaceUtils.PHASE};

		for (String attribute : attributes) {
			IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, attribute);
		}
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "subscription-mode", true);
	}

}
