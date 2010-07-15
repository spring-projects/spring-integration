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
		builder.addConstructorArgValue(element.getAttribute("url"));
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "http-method");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "message-converters");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "parameter-extractor");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "charset");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "extract-payload");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "expected-response-type");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "request-factory");
		return builder.getBeanDefinition();
	}

}
