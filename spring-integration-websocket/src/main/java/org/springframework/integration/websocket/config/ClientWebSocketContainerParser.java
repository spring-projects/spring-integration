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
				.addConstructorArgValue(element.getAttribute("uri"));
		String uriVariables = element.getAttribute("uri-variables");
		if (StringUtils.hasText(uriVariables)) {
			builder.addConstructorArgValue(StringUtils.commaDelimitedListToStringArray(uriVariables));
		}
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "send-buffer-size-limit");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "send-time-limit");
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
