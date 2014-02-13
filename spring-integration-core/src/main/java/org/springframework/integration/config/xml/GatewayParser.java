/*
 * Copyright 2002-2014 the original author or authors.
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

package org.springframework.integration.config.xml;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanDefinitionHolder;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.MessagingGatewayRegistrar;
import org.springframework.integration.gateway.GatewayMethodMetadata;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * Parser for the &lt;gateway/&gt; element.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @author Artem Bilan
 */
public class GatewayParser implements BeanDefinitionParser {

	private final MessagingGatewayRegistrar registrar = new MessagingGatewayRegistrar();

	@Override
	@SuppressWarnings("rawtypes")
	public BeanDefinition parse(final Element element, ParserContext parserContext) {
		boolean isNested = parserContext.isNested();

		final Map<String, Object> gatewayAttributes = new HashMap<String, Object>();
		gatewayAttributes.put("name", element.getAttribute(AbstractBeanDefinitionParser.ID_ATTRIBUTE));
		gatewayAttributes.put("defaultPayloadExpression", element.getAttribute("default-payload-expression"));
		gatewayAttributes.put("defaultRequestChannel", element.getAttribute(isNested ? "request-channel" : "default-request-channel"));
		gatewayAttributes.put("defaultReplyChannel", element.getAttribute(isNested ? "reply-channel" : "default-reply-channel"));
		gatewayAttributes.put("errorChannel", element.getAttribute("error-channel"));
		gatewayAttributes.put("asyncExecutor", element.getAttribute("async-executor"));
		gatewayAttributes.put("mapper", element.getAttribute("mapper"));
		gatewayAttributes.put("defaultReplyTimeout", element.getAttribute(isNested ? "reply-timeout" : "default-reply-timeout"));
		gatewayAttributes.put("defaultRequestTimeout", element.getAttribute(isNested ? "request-timeout" : "default-request-timeout"));


		List<Element> headerElements = DomUtils.getChildElementsByTagName(element, "default-header");
		if (!CollectionUtils.isEmpty(headerElements)) {
			List<Map<String, Object>> headers = new ArrayList<Map<String, Object>>(headerElements.size());
			for (Element e : headerElements) {
				Map<String, Object> header = new HashMap<String, Object>();
				header.put("name", e.getAttribute("name"));
				header.put("value", e.getAttribute("value"));
				header.put("expression", e.getAttribute("expression"));
				headers.add(header);
			}
			gatewayAttributes.put("defaultHeaders", headers.toArray(new Map[headers.size()]));
		}

		List<Element> methodElements = DomUtils.getChildElementsByTagName(element, "method");
		if (!CollectionUtils.isEmpty(methodElements)) {
			Map<String, BeanDefinition> methodMetadataMap = new ManagedMap<String, BeanDefinition>();
			for (Element methodElement : methodElements) {
				String methodName = methodElement.getAttribute("name");
				BeanDefinitionBuilder methodMetadataBuilder = BeanDefinitionBuilder.genericBeanDefinition(
						GatewayMethodMetadata.class);
				methodMetadataBuilder.addPropertyValue("requestChannelName", methodElement.getAttribute("request-channel"));
				methodMetadataBuilder.addPropertyValue("replyChannelName", methodElement.getAttribute("reply-channel"));
				methodMetadataBuilder.addPropertyValue("requestTimeout", methodElement.getAttribute("request-timeout"));
				methodMetadataBuilder.addPropertyValue("replyTimeout", methodElement.getAttribute("reply-timeout"));

				boolean hasMapper = StringUtils.hasText(element.getAttribute("mapper"));
				Assert.state(!hasMapper || !StringUtils.hasText(element.getAttribute("payload-expression")),
						"'payload-expression' is not allowed when a 'mapper' is provided");

				IntegrationNamespaceUtils.setValueIfAttributeDefined(methodMetadataBuilder, methodElement, "payload-expression");

				List<Element> invocationHeaders = DomUtils.getChildElementsByTagName(methodElement, "header");
				if (!CollectionUtils.isEmpty(invocationHeaders)) {
					Assert.state(!hasMapper, "header elements are not allowed when a 'mapper' is provided");

					Map<String, Object> headerExpressions = new ManagedMap<String, Object>();
					for (Element headerElement : invocationHeaders) {
						BeanDefinition expressionDef = IntegrationNamespaceUtils
								.createExpressionDefinitionFromValueOrExpression("value", "expression", parserContext, headerElement, true);

						headerExpressions.put(headerElement.getAttribute("name"), expressionDef);
					}
					methodMetadataBuilder.addPropertyValue("headerExpressions", headerExpressions);
				}
				methodMetadataMap.put(methodName, methodMetadataBuilder.getBeanDefinition());
			}

			gatewayAttributes.put("methods", methodMetadataMap);
		}

		gatewayAttributes.put("serviceInterface", element.getAttribute("service-interface"));

		BeanDefinitionHolder gatewayHolder = this.registrar.parse(gatewayAttributes);
		if (isNested) {
			return gatewayHolder.getBeanDefinition();
		}
		else {
			BeanDefinitionReaderUtils.registerBeanDefinition(gatewayHolder, parserContext.getRegistry());
			return null;
		}
	}

}
