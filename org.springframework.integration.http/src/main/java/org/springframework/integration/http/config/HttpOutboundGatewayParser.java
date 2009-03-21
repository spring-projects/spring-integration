/*
 * Copyright 2002-2009 the original author or authors.
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

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractConsumerEndpointParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.util.StringUtils;

/**
 * Parser for the 'outbound-gateway' element of the http namespace.
 * 
 * @author Mark Fisher
 */
public class HttpOutboundGatewayParser extends AbstractConsumerEndpointParser {

	private static final String PACKAGE_PATH = "org.springframework.integration.http";


	@Override
	protected String getInputChannelAttributeName() {
		return "request-channel";
	}

	@Override
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
		String defaultUrl = element.getAttribute("default-url");
		String charset = element.getAttribute("charset");
		String extractPayload = element.getAttribute("extract-request-payload");
		String requestMapperRef = element.getAttribute("request-mapper");
		if (StringUtils.hasText(requestMapperRef)) {
			if (StringUtils.hasText(defaultUrl)) {
				this.requestMapperConflictError("default-url", parserContext, element);
				return null;
			}
			else if (StringUtils.hasText(charset)) {
				this.requestMapperConflictError("charset", parserContext, element);
				return null;
			}
			else if (StringUtils.hasText(extractPayload)) {
				this.requestMapperConflictError("extract-request-payload", parserContext, element);
				return null;
			}
		}
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(
				PACKAGE_PATH + ".HttpOutboundEndpoint");
		if (!StringUtils.hasText(requestMapperRef)) {
			BeanDefinitionBuilder mapperBuilder = BeanDefinitionBuilder.genericBeanDefinition(
					PACKAGE_PATH + ".DefaultOutboundRequestMapper");
			if (StringUtils.hasText(defaultUrl)) {
				mapperBuilder.addConstructorArgValue(defaultUrl);
			}
			if (StringUtils.hasText(charset)) {
				mapperBuilder.addPropertyValue("charset", charset);
			}
			if (StringUtils.hasText(extractPayload)) {
				mapperBuilder.addPropertyValue("extractPayload", extractPayload);
			}
			requestMapperRef = BeanDefinitionReaderUtils.registerWithGeneratedName(
					mapperBuilder.getBeanDefinition(), parserContext.getRegistry());
		}
		builder.addPropertyReference("requestMapper", requestMapperRef);
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "request-timeout", "sendTimeout");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "request-executor");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "reply-channel", "outputChannel");
		return builder;
	}

	private void requestMapperConflictError(String nameForGateway, ParserContext parserContext, Element element) {
		parserContext.getReaderContext().error("The '" + nameForGateway + "' and 'request-mapper' are mutually exclusive. " +
				"When providing an OutboundRequestMapper, set any corresponding property on the mapper directly.", element);
	}

}
