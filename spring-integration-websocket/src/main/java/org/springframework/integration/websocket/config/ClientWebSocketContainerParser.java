/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.websocket.config;

import java.util.Map;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.websocket.ClientWebSocketContainer;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * The {@link AbstractSingleBeanDefinitionParser} implementation for
 * the {@code <websocket:client-container/>} element.
 *
 * @author Artem Bilan
 * @author Julian Koch
 * @since 4.1
 */
public class ClientWebSocketContainerParser extends AbstractSingleBeanDefinitionParser {

	@Override
	protected Class<?> getBeanClass(Element element) {
		return ClientWebSocketContainer.class;
	}

	@Override
	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		builder.addConstructorArgReference(element.getAttribute("client"))
				.addConstructorArgValue(element.getAttribute("uri"))
				.addConstructorArgValue(StringUtils.commaDelimitedListToStringArray(element.getAttribute("uri-variables")));

		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "send-buffer-size-limit");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "send-time-limit");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "send-buffer-overflow-strategy");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "origin");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, IntegrationNamespaceUtils.AUTO_STARTUP);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, IntegrationNamespaceUtils.PHASE);

		Element httpHeaders = DomUtils.getChildElementByTagName(element, "http-headers");
		if (httpHeaders != null) {
			Map<?, ?> map = parserContext.getDelegate().parseMapElement(httpHeaders, builder.getBeanDefinition());
			builder.addPropertyValue("headersMap", map);
		}

	}

}
