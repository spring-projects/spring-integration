/*
 * Copyright © 2014 Broadcom Inc. and/or its subsidiaries. All Rights Reserved.
 * Copyright 2014-present the original author or authors.
 */

package org.springframework.integration.websocket.config;

import java.util.List;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanReference;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.websocket.support.SubProtocolHandlerRegistry;
import org.springframework.util.StringUtils;

/**
 * @author Artem Bilan
 * @since 4.1
 */
abstract class WebSocketAdapterParsingUtils {

	static void configureWebSocketAdapter(BeanDefinitionBuilder builder, ParserContext parserContext, Element element) {
		String container = element.getAttribute("container");
		if (!StringUtils.hasText(container)) {
			parserContext.getReaderContext().error("The 'container' is required", element);
		}
		builder.addConstructorArgReference(container);

		String protocolHandlers = element.getAttribute("protocol-handlers");
		boolean hasProtocolHandlers = StringUtils.hasText(protocolHandlers);
		String defaultProtocolHandler = element.getAttribute("default-protocol-handler");
		boolean hasDefaultProtocolHandler = StringUtils.hasText(defaultProtocolHandler);

		if (hasProtocolHandlers || hasDefaultProtocolHandler) {
			List<BeanReference> protocolHandlerList = new ManagedList<BeanReference>();
			String[] ids = StringUtils.commaDelimitedListToStringArray(protocolHandlers);
			for (String id : ids) {
				protocolHandlerList.add(new RuntimeBeanReference(id));
			}
			BeanDefinitionBuilder protocolHandlerRegistryBuilder =
					BeanDefinitionBuilder.genericBeanDefinition(SubProtocolHandlerRegistry.class)
							.addConstructorArgValue(protocolHandlerList);
			if (hasDefaultProtocolHandler) {
				protocolHandlerRegistryBuilder.addConstructorArgReference(defaultProtocolHandler);
			}
			builder.addConstructorArgValue(protocolHandlerRegistryBuilder.getBeanDefinition());
		}

		String messageConverters = element.getAttribute("message-converters");
		if (StringUtils.hasText(messageConverters)) {
			List<BeanReference> messageConverterList = new ManagedList<BeanReference>();
			String[] ids = StringUtils.commaDelimitedListToStringArray(messageConverters);
			for (String id : ids) {
				messageConverterList.add(new RuntimeBeanReference(id));
			}
			builder.addPropertyValue("messageConverters", messageConverterList);
		}

		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "merge-with-default-converters");
	}

}
