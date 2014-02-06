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

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.handler.LoggingHandler;
import org.springframework.util.StringUtils;

/**
 * Parser for the 'logging-channel-adapter' element.
 *
 * @author Mark Fisher
 * @since 1.0.1
 */
public class LoggingChannelAdapterParser extends AbstractOutboundChannelAdapterParser {

	@Override
	protected AbstractBeanDefinition parseConsumer(Element element, ParserContext parserContext) {
		Object source = parserContext.extractSource(element);
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.genericBeanDefinition(LoggingHandler.class);
		builder.addConstructorArgValue(element.getAttribute("level"));
		String expression = element.getAttribute("expression");
		String logFullMessage = element.getAttribute("log-full-message");
		if (StringUtils.hasText(logFullMessage)) {
			if (StringUtils.hasText(expression)) {
				parserContext.getReaderContext().error(
						"The 'expression' and 'log-full-message' attributes are mutually exclusive.", source);
			}
			builder.addPropertyValue("shouldLogFullMessage", logFullMessage);
		}
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "logger-name");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "expression");
		return builder.getBeanDefinition();
	}

}
