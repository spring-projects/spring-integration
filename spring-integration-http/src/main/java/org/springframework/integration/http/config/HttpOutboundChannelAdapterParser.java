/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.http.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractOutboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.util.xml.DomUtils;

/**
 * Parser for the 'outbound-channel-adapter' element of the http namespace.
 * 
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class HttpOutboundChannelAdapterParser extends AbstractOutboundChannelAdapterParser {

	@Override
	protected AbstractBeanDefinition parseConsumer(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(
				"org.springframework.integration.http.outbound.HttpRequestExecutingMessageHandler");
		builder.addPropertyValue("expectReply", false);
		builder.addConstructorArgValue(element.getAttribute("url"));
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "http-method");
		
		String restTemplate = element.getAttribute("rest-template");
		if (StringUtils.hasText(restTemplate)){
			HttpAdapterParsingUtils.verifyNoRestTemplateAttributes(element, parserContext);
			builder.addConstructorArgReference("restTemplate");
		}
		else {
			IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "message-converters");
			IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "error-handler");
			IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "request-factory");
		}
		
		String headerMapper = element.getAttribute("header-mapper");
		String mappedRequestHeaders = element.getAttribute("mapped-request-headers");
		if (StringUtils.hasText(headerMapper)) {
			if (StringUtils.hasText(mappedRequestHeaders)) {
				parserContext.getReaderContext().error("The 'mappped-request-headers' attribute is not " +
						"allowed when a 'header-mapper' has been specified.", parserContext.extractSource(element));
				return null;
			}
			builder.addPropertyReference("headerMapper", headerMapper);
		}
		else if (StringUtils.hasText(mappedRequestHeaders)) {
			BeanDefinitionBuilder headerMapperBuilder = BeanDefinitionBuilder.genericBeanDefinition(
					"org.springframework.integration.http.support.DefaultHttpHeaderMapper");
			if (StringUtils.hasText(mappedRequestHeaders)) {
				IntegrationNamespaceUtils.setValueIfAttributeDefined(headerMapperBuilder, element, "mapped-request-headers", "outboundHeaderNames");
			}	
			builder.addPropertyValue("headerMapper", headerMapperBuilder.getBeanDefinition());
		}
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "charset");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "extract-payload");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "expected-response-type");
		List<Element> uriVariableElements = DomUtils.getChildElementsByTagName(element, "uri-variable");
		if (!CollectionUtils.isEmpty(uriVariableElements)) {
			Map<String, String> uriVariableExpressions = new HashMap<String, String>();
			for (Element uriVariableElement : uriVariableElements) {
				String name = uriVariableElement.getAttribute("name");
				String expression = uriVariableElement.getAttribute("expression");
				uriVariableExpressions.put(name, expression);
			}
			builder.addPropertyValue("uriVariableExpressions", uriVariableExpressions);
		}
		return builder.getBeanDefinition();
	}

}
