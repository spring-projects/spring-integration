/*
 * Copyright 2002-2008 the original author or authors.
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

import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.support.ManagedMap;
import org.springframework.beans.factory.xml.AbstractBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.aop.MethodNameMappingExpressionSource;
import org.springframework.integration.channel.MapBasedChannelResolver;
import org.springframework.integration.context.IntegrationContextUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;
import org.w3c.dom.Element;

/**
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class PublisherParser extends AbstractBeanDefinitionParser {

	/**
	 * 
	 */
	protected AbstractBeanDefinition parseInternal(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder rootBuilder = BeanDefinitionBuilder.genericBeanDefinition(
				IntegrationNamespaceUtils.BASE_PACKAGE + ".aop.MessagePublishingInterceptor");
		
		BeanDefinitionBuilder spelSourceBilder = BeanDefinitionBuilder.genericBeanDefinition(MethodNameMappingExpressionSource.class.getName());
		Map<String, Map<?,?>> mappings = this.getMappings(element, element.getAttribute("default-channel"));
		spelSourceBilder.addConstructorArgValue(mappings.get("payload"));
		if (mappings.get("headers") != null){
			spelSourceBilder.addPropertyValue("headerExpressionMap", mappings.get("headers"));
		}
		BeanDefinitionBuilder chResolverBuilder = BeanDefinitionBuilder.genericBeanDefinition(MapBasedChannelResolver.class.getName());
		if (mappings.get("channels") != null){
			spelSourceBilder.addPropertyValue("channelMap", mappings.get("channels"));
			chResolverBuilder.addConstructorArgValue(mappings.get("resolvableChannels"));
		}
		String chResolverName = 
			BeanDefinitionReaderUtils.registerWithGeneratedName(chResolverBuilder.getBeanDefinition(), parserContext.getRegistry());
		String defaultChannel = StringUtils.hasText(element.getAttribute("default-channel")) ? 
				element.getAttribute("default-channel") : IntegrationContextUtils.NULL_CHANNEL_BEAN_NAME;
				
		String spelSourceName = 
			BeanDefinitionReaderUtils.registerWithGeneratedName(spelSourceBilder.getBeanDefinition(), parserContext.getRegistry());	
		rootBuilder.addConstructorArgReference(spelSourceName);	
		
		rootBuilder.addPropertyReference("channelResolver", chResolverName);
		
		rootBuilder.addPropertyReference("defaultChannel", defaultChannel);

		return rootBuilder.getBeanDefinition();
	}
	
	@SuppressWarnings("unchecked")
	private Map<String,Map<?,?>> getMappings(Element element, String defaultChannel){
		List<Element> mappings = DomUtils.getChildElementsByTagName(element, "method");
		Map<String, Map<?,?>> interceptorMappings = new HashMap<String, Map<?,?>>();
		Map<String, String> payloadExpressionMap = new HashMap<String, String>();
		Map<String, String[]> headersExpressionMap = new HashMap<String, String[]>();
		Map<String, String> channelMap = new HashMap<String, String>();
		ManagedMap resolvableChannelMap = new ManagedMap();
		if (mappings != null && mappings.size() > 0){	
			for (Element mapping : mappings) {
				// set payloadMap
				String methodPattern = StringUtils.hasText(mapping.getAttribute("pattern")) ? 
															mapping.getAttribute("pattern") : "*" ;
				String payloadExpression = StringUtils.hasText(mapping.getAttribute("payload")) ? 
															mapping.getAttribute("payload") : "#return" ;
				payloadExpressionMap.put(methodPattern, payloadExpression);
				// set headersMap
				String headersExpression = mapping.getAttribute("headers");
				if (StringUtils.hasText(headersExpression)){
					headersExpressionMap.put(methodPattern, StringUtils.commaDelimitedListToStringArray(headersExpression));
				}
				// set channelMap
				String tmpChannel = mapping.getAttribute("channel");
				String channel = StringUtils.hasText(tmpChannel) ? tmpChannel : defaultChannel;
				channelMap.put(methodPattern, channel);
				resolvableChannelMap.put(channel, new RuntimeBeanReference(channel));
			}
		} 
		
		if (payloadExpressionMap.size() == 0){
			payloadExpressionMap.put("*", "#return");
		} 
		interceptorMappings.put("payload", payloadExpressionMap);
		
		if (headersExpressionMap.size() > 0){
			interceptorMappings.put("headers", headersExpressionMap);
		}
		if (channelMap.size() > 0){
			interceptorMappings.put("channels", channelMap);
			interceptorMappings.put("resolvableChannels", resolvableChannelMap);
		}
		return interceptorMappings;
	}
}
