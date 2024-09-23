/*
 * Copyright 2002-2024 the original author or authors.
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

package org.springframework.integration.config.xml;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.aop.MessagePublishingInterceptor;
import org.springframework.integration.aop.MethodNameMappingPublisherMetadataSource;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * Parser for the &lt;publishing-interceptor&gt; element.
 *
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Gary Russell
 * @author Artem Bilan
 * @author Ngoc Nhan
 * @since 2.0
 */
public class PublishingInterceptorParser extends AbstractBeanDefinitionParser {

	private static final String PAYLOAD = "payload";

	@Override
	protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder rootBuilder = BeanDefinitionBuilder.genericBeanDefinition(
				MessagePublishingInterceptor.class);
		BeanDefinitionBuilder spelSourceBuilder = BeanDefinitionBuilder
				.genericBeanDefinition(MethodNameMappingPublisherMetadataSource.class);
		Map<String, Map<?, ?>> mappings = this
				.getMappings(element, element.getAttribute("default-channel"), parserContext);
		spelSourceBuilder.addConstructorArgValue(mappings.get(PAYLOAD));
		if (mappings.get("headers") != null) {
			spelSourceBuilder.addPropertyValue("headerExpressionMap", mappings.get("headers"));
		}

		if (mappings.get("channels") != null) {
			spelSourceBuilder.addPropertyValue("channelMap", mappings.get("channels"));
		}
		String defaultChannel = StringUtils.hasText(element.getAttribute("default-channel")) ?
				element.getAttribute("default-channel") : IntegrationContextUtils.NULL_CHANNEL_BEAN_NAME;
		rootBuilder.addConstructorArgValue(spelSourceBuilder.getBeanDefinition());
		rootBuilder.addPropertyValue("defaultChannelName", defaultChannel);
		return rootBuilder.getBeanDefinition();
	}

	private Map<String, Map<?, ?>> getMappings(Element element, String defaultChannel, ParserContext parserContext) {
		List<Element> mappings = DomUtils.getChildElementsByTagName(element, "method");
		Map<String, Map<?, ?>> interceptorMappings = new HashMap<>();
		Map<String, String> payloadExpressionMap = new HashMap<>();
		Map<String, Map<String, String>> headersExpressionMap = new HashMap<>();
		Map<String, String> channelMap = new HashMap<>();
		ManagedMap<String, Object> resolvableChannelMap = new ManagedMap<>();
		if (!CollectionUtils.isEmpty(mappings)) {
			for (Element mapping : mappings) {
				// set payloadMap
				String methodPattern = StringUtils.hasText(mapping.getAttribute("pattern")) ?
						mapping.getAttribute("pattern") : "*";
				String payloadExpression = StringUtils.hasText(mapping.getAttribute(PAYLOAD)) ?
						mapping.getAttribute(PAYLOAD) : "#return";
				payloadExpressionMap.put(methodPattern, payloadExpression);

				// set headersMap
				Map<String, String> headerExpressions = headerExpressions(parserContext, mapping);
				if (!headerExpressions.isEmpty()) {
					headersExpressionMap.put(methodPattern, headerExpressions);
				}

				// set channelMap
				String tmpChannel = mapping.getAttribute("channel");
				String channel = StringUtils.hasText(tmpChannel) ? tmpChannel : defaultChannel;
				channelMap.put(methodPattern, channel);
				resolvableChannelMap.put(channel, new RuntimeBeanReference(channel));
			}
		}
		if (payloadExpressionMap.isEmpty()) {
			payloadExpressionMap.put("*", "#return");
		}
		interceptorMappings.put(PAYLOAD, payloadExpressionMap);
		if (!headersExpressionMap.isEmpty()) {
			interceptorMappings.put("headers", headersExpressionMap);
		}
		if (!channelMap.isEmpty()) {
			interceptorMappings.put("channels", channelMap);
			interceptorMappings.put("resolvableChannels", resolvableChannelMap);
		}
		return interceptorMappings;
	}

	private Map<String, String> headerExpressions(ParserContext parserContext, Element mapping) {
		List<Element> headerElements = DomUtils.getChildElementsByTagName(mapping, "header");
		Map<String, String> headerExpressions = new HashMap<>();
		for (Element headerElement : headerElements) {
			String name = headerElement.getAttribute("name");
			if (!StringUtils.hasText(name)) {
				parserContext.getReaderContext()
						.error("the 'name' attribute is required on the <header> element",
								parserContext.extractSource(headerElement));
				continue;
			}
			String value = headerElement.getAttribute("value");
			String expression = headerElement.getAttribute("expression");
			boolean hasValue = StringUtils.hasText(value);
			boolean hasExpression = StringUtils.hasText(expression);
			if (hasValue == hasExpression) {
				parserContext.getReaderContext()
						.error("exactly one of 'value' or 'expression' is required on the <header> element",
								parserContext.extractSource(headerElement));
				continue;
			}
			if (hasValue) {
				expression = "'" + value + "'";
			}
			headerExpressions.put(name, expression);
		}
		return headerExpressions;
	}

}
