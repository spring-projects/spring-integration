/*
 * Copyright 2002-2013 the original author or authors.
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.aop.MessagePublishingInterceptor;
import org.springframework.integration.aop.MethodNameMappingPublisherMetadataSource;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.integration.support.channel.BeanFactoryChannelResolver;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * Parser for the &lt;publishing-interceptor&gt; element.
 *
 * @author Oleg Zhurakousky
 * @author Mark Fisher
 * @author Gary Russell
 * @since 2.0
 */
public class PublishingInterceptorParser extends AbstractBeanDefinitionParser {

	@Override
	protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder rootBuilder = BeanDefinitionBuilder.genericBeanDefinition(
				MessagePublishingInterceptor.class);
		BeanDefinitionBuilder spelSourceBuilder = BeanDefinitionBuilder
				.genericBeanDefinition(MethodNameMappingPublisherMetadataSource.class);
		Map<String, Map<?,?>> mappings = this.getMappings(element, element.getAttribute("default-channel"), parserContext);
		spelSourceBuilder.addConstructorArgValue(mappings.get("payload"));
		if (mappings.get("headers") != null) {
			spelSourceBuilder.addPropertyValue("headerExpressionMap", mappings.get("headers"));
		}

		BeanDefinitionBuilder chResolverBuilder = BeanDefinitionBuilder.genericBeanDefinition(
				BeanFactoryChannelResolver.class);

		if (mappings.get("channels") != null){
			spelSourceBuilder.addPropertyValue("channelMap", mappings.get("channels"));
		}
		String chResolverName =
				BeanDefinitionReaderUtils.registerWithGeneratedName(chResolverBuilder.getBeanDefinition(), parserContext.getRegistry());
		String defaultChannel = StringUtils.hasText(element.getAttribute("default-channel")) ?
				element.getAttribute("default-channel") : IntegrationContextUtils.NULL_CHANNEL_BEAN_NAME;
		rootBuilder.addConstructorArgValue(spelSourceBuilder.getBeanDefinition());
		rootBuilder.addPropertyReference("channelResolver", chResolverName);
		rootBuilder.addPropertyReference("defaultChannel", defaultChannel);
		return rootBuilder.getBeanDefinition();
	}

	private Map<String,Map<?,?>> getMappings(Element element, String defaultChannel, ParserContext parserContext) {
		List<Element> mappings = DomUtils.getChildElementsByTagName(element, "method");
		Map<String, Map<?,?>> interceptorMappings = new HashMap<String, Map<?,?>>();
		Map<String, String> payloadExpressionMap = new HashMap<String, String>();
		Map<String, Map<String, String>> headersExpressionMap = new HashMap<String, Map<String, String>>();
		Map<String, String> channelMap = new HashMap<String, String>();
		ManagedMap<String, Object> resolvableChannelMap = new ManagedMap<String, Object>();
		if (mappings != null && mappings.size() > 0) {
			for (Element mapping : mappings) {
				// set payloadMap
				String methodPattern = StringUtils.hasText(mapping.getAttribute("pattern")) ?
						mapping.getAttribute("pattern") : "*";
				String payloadExpression = StringUtils.hasText(mapping.getAttribute("payload")) ?
						mapping.getAttribute("payload") : "#return";
				payloadExpressionMap.put(methodPattern, payloadExpression);

				// set headersMap
				List<Element> headerElements = DomUtils.getChildElementsByTagName(mapping, "header");
				Map<String, String> headerExpressions = new HashMap<String, String>();
				for (Element headerElement : headerElements) {
					String name = headerElement.getAttribute("name");
					if (!StringUtils.hasText(name)) {
						parserContext.getReaderContext().error("the 'name' attribute is required on the <header> element",
								parserContext.extractSource(headerElement));
						continue;
					}
					String value = headerElement.getAttribute("value");
					String expression = headerElement.getAttribute("expression");
					boolean hasValue = StringUtils.hasText(value);
					boolean hasExpression = StringUtils.hasText(expression);
					if (!(hasValue ^ hasExpression)) {
						parserContext.getReaderContext().error("exactly one of 'value' or 'expression' is required on the <header> element",
								parserContext.extractSource(headerElement));
						continue;
					}
					if (hasValue) {
						expression = "'" + value + "'";
					}
					headerExpressions.put(name, expression);
				}
				if (headerExpressions.size() > 0) {
					headersExpressionMap.put(methodPattern, headerExpressions);
				}

				// set channelMap
				String tmpChannel = mapping.getAttribute("channel");
				String channel = StringUtils.hasText(tmpChannel) ? tmpChannel : defaultChannel;
				channelMap.put(methodPattern, channel);
				resolvableChannelMap.put(channel, new RuntimeBeanReference(channel));
			}
		}
		if (payloadExpressionMap.size() == 0) {
			payloadExpressionMap.put("*", "#return");
		}
		interceptorMappings.put("payload", payloadExpressionMap);
		if (headersExpressionMap.size() > 0) {
			interceptorMappings.put("headers", headersExpressionMap);
		}
		if (channelMap.size() > 0) {
			interceptorMappings.put("channels", channelMap);
			interceptorMappings.put("resolvableChannels", resolvableChannelMap);
		}
		return interceptorMappings;
	}

}
