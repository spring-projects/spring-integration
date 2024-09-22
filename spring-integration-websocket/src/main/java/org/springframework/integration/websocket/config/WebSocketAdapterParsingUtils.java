/*
 * Copyright 2014-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 * @author Ngoc Nhan
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
			List<BeanReference> protocolHandlerList = new ManagedList<>();
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
			List<BeanReference> messageConverterList = new ManagedList<>();
			String[] ids = StringUtils.commaDelimitedListToStringArray(messageConverters);
			for (String id : ids) {
				messageConverterList.add(new RuntimeBeanReference(id));
			}
			builder.addPropertyValue("messageConverters", messageConverterList);
		}

		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "merge-with-default-converters");
	}

}
