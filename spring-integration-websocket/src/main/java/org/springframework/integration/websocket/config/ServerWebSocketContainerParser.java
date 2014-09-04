/*
 * Copyright 2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.websocket.ServerWebSocketContainer;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * The {@link org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser} implementation for
 * the {@code <websocket:server-container/>} element.
 *
 * @author Artem Bilan
 * @since 4.1
 */
public class ServerWebSocketContainerParser extends AbstractSingleBeanDefinitionParser {

	@Override
	protected Class<?> getBeanClass(Element element) {
		return ServerWebSocketContainer.class;
	}

	@Override
	protected boolean shouldGenerateIdAsFallback() {
		return true;
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		builder.addConstructorArgValue(element.getAttribute("path"));

		String handshakeInterceptors = element.getAttribute("handshake-interceptors");
		List<BeanReference> handshakeInterceptorList = new ManagedList<BeanReference>();
		String[] ids = StringUtils.commaDelimitedListToStringArray(handshakeInterceptors);
		for (String id : ids) {
			handshakeInterceptorList.add(new RuntimeBeanReference(id));
		}
		builder.addPropertyValue("interceptors", handshakeInterceptorList);

		Element sockjs = DomUtils.getChildElementByTagName(element, "sockjs");

		if (sockjs != null) {
			BeanDefinitionBuilder sockjsBuilder =
					BeanDefinitionBuilder.genericBeanDefinition(ServerWebSocketContainer.SockJsServiceOptions.class);
			IntegrationNamespaceUtils.setValueIfAttributeDefined(sockjsBuilder, sockjs, "client-library-url");
			IntegrationNamespaceUtils.setValueIfAttributeDefined(sockjsBuilder, sockjs, "websocket-enabled",
					"webSocketEnabled");
			IntegrationNamespaceUtils.setValueIfAttributeDefined(sockjsBuilder, sockjs, "stream-bytes-limit");
			IntegrationNamespaceUtils.setValueIfAttributeDefined(sockjsBuilder, sockjs, "session-cookie-needed");
			IntegrationNamespaceUtils.setValueIfAttributeDefined(sockjsBuilder, sockjs, "heartbeat-time");
			IntegrationNamespaceUtils.setValueIfAttributeDefined(sockjsBuilder, sockjs, "disconnect-delay");
			IntegrationNamespaceUtils.setValueIfAttributeDefined(sockjsBuilder, sockjs, "message-cache-size",
					"httpMessageCacheSize");
			IntegrationNamespaceUtils.setReferenceIfAttributeDefined(sockjsBuilder, sockjs, "scheduler",
					"taskScheduler");
			IntegrationNamespaceUtils.setReferenceIfAttributeDefined(sockjsBuilder, sockjs, "message-codec");

			String transportHandlers = sockjs.getAttribute("transport-handlers");
			if (StringUtils.hasText(transportHandlers)) {
				List<BeanReference> transportHandlerList = new ManagedList<BeanReference>();
				ids = StringUtils.commaDelimitedListToStringArray(transportHandlers);
				for (String id : ids) {
					transportHandlerList.add(new RuntimeBeanReference(id));
				}
				sockjsBuilder.addPropertyValue("transportHandlers", transportHandlerList);
			}
			builder.addPropertyValue("sockJsServiceOptions", sockjsBuilder.getBeanDefinition());
		}

		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "handshake-handler");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "send-buffer-size-limit");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "send-time-limit");
	}

}
