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

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;

/**
 * Base class for url-based outbound gateway parsers.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 */
public abstract class AbstractOutboundGatewayParser extends AbstractConsumerEndpointParser {

	protected abstract String getGatewayClassName(Element element);

	@Override
	protected String getInputChannelAttributeName() {
		return "request-channel";
	}

	@Override
	protected BeanDefinitionBuilder parseHandler(Element element, ParserContext parserContext) {
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(this.getGatewayClassName(element));
		String url = this.parseUrl(element, parserContext);
		builder.addConstructorArgValue(url);
		String replyChannel = element.getAttribute("reply-channel");
		if (StringUtils.hasText(replyChannel)) {
			builder.addPropertyReference("replyChannel", replyChannel);
		}
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "requires-reply");
		this.postProcessGateway(builder, element, parserContext);
		return builder;
	}

	protected String parseUrl(Element element, ParserContext parserContext) {
		String url = element.getAttribute("url");
		if (!StringUtils.hasText(url)) {
			parserContext.getReaderContext().error("The 'url' attribute is required.", element);
		}
		return url;
	}

	/**
	 * Subclasses may override this method for additional configuration.

	 * @param builder The builder.
	 * @param element The element.
	 * @param parserContext The parser context.
	 */
	protected void postProcessGateway(BeanDefinitionBuilder builder, Element element, ParserContext parserContext) {
	}

}
