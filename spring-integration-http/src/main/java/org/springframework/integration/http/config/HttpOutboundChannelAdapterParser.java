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

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractOutboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.util.StringUtils;

/**
 * Parser for the 'outbound-channel-adapter' element of the http namespace.
 * 
 * @author Mark Fisher
 * @since 2.0
 */
public class HttpOutboundChannelAdapterParser extends AbstractOutboundChannelAdapterParser {

	private static final String PACKAGE_PATH = "org.springframework.integration.http";

	@Override
	protected AbstractBeanDefinition parseConsumer(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(
				PACKAGE_PATH + ".HttpRequestExecutingMessageHandler");
		builder.addPropertyValue("expectReply", false);
		String url = element.getAttribute("url");
		if (StringUtils.hasText(url)) {
			builder.addConstructorArgValue(url);
		}
		BeanDefinitionBuilder mapperBuilder = BeanDefinitionBuilder.genericBeanDefinition(
				PACKAGE_PATH + ".DefaultOutboundRequestMapper");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(mapperBuilder, element, "charset");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(mapperBuilder, element, "extract-payload");
		builder.addPropertyValue("requestMapper", mapperBuilder.getBeanDefinition());
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "request-factory");
		return builder.getBeanDefinition();
	}

}
