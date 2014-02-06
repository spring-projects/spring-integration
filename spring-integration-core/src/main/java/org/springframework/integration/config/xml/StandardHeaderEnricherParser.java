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

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.IntegrationMessageHeaderAccessor;
import org.springframework.integration.handler.MethodInvokingMessageProcessor;
import org.springframework.messaging.MessageHeaders;
import org.springframework.util.StringUtils;

/**
 * Parser for the &lt;header-enricher&gt; element within the core integration
 * namespace. This is used for setting the <em>standard</em>, out-of-the-box
 * configurable {@link MessageHeaders}, such as 'reply-channel', 'priority',
 * and 'correlation-id'. It will also accept custom header values (or bean
 * references) if provided as 'header' sub-elements.
 *
 * @author Mark Fisher
 * @author Oleg Zhurakousky
 */
public class StandardHeaderEnricherParser extends HeaderEnricherParserSupport {

	public StandardHeaderEnricherParser() {
		this.addElementToHeaderMapping("reply-channel", MessageHeaders.REPLY_CHANNEL);
		this.addElementToHeaderMapping("error-channel", MessageHeaders.ERROR_CHANNEL);
		this.addElementToHeaderMapping("correlation-id", IntegrationMessageHeaderAccessor.CORRELATION_ID);
		this.addElementToHeaderMapping("expiration-date", IntegrationMessageHeaderAccessor.EXPIRATION_DATE, Long.class);
		this.addElementToHeaderMapping("priority", IntegrationMessageHeaderAccessor.PRIORITY, Integer.class);
	}

	@Override
	protected void postProcessHeaderEnricher(BeanDefinitionBuilder builder, Element element, ParserContext parserContext) {
		String ref = element.getAttribute("ref");
		String method = element.getAttribute("method");
		if (StringUtils.hasText(ref) || StringUtils.hasText(method)) {
			if (!StringUtils.hasText(ref) || !StringUtils.hasText(method)) {
				parserContext.getReaderContext().error(
						"If either 'ref' or 'method' is provided, then they are both required.",
						parserContext.extractSource(element));
				return;
			}
			BeanDefinitionBuilder processorBuilder = BeanDefinitionBuilder.genericBeanDefinition(MethodInvokingMessageProcessor.class);
			processorBuilder.addConstructorArgReference(ref);
			processorBuilder.addConstructorArgValue(method);
			builder.addPropertyValue("messageProcessor", processorBuilder.getBeanDefinition());
		}
	}

}
