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

package org.springframework.integration.jms.config;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.AbstractSingleBeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.util.StringUtils;

/**
 * Parser for the 'channel' and 'publish-subscribe-channel' elements of the
 * Spring Integration JMS namespace.
 * 
 * @author Mark Fisher
 * @since 2.0
 */
public class JmsChannelParser extends AbstractSingleBeanDefinitionParser {

	@Override
	protected String getBeanClassName(Element element) {
		return "org.springframework.integration.jms.JmsDestinationBackedMessageChannel";
	}

	@Override
	protected void doParse(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String connectionFactory = element.getAttribute("connection-factory");
		if (!StringUtils.hasText(connectionFactory)) {
			connectionFactory = "connectionFactory";
		}
		builder.addConstructorArgReference(connectionFactory);
		if ("channel".equals(element.getLocalName())) {
			this.parseDestination(element, parserContext, builder, "queue");
		}
		else if ("publish-subscribe-channel".equals(element.getLocalName())) {
			this.parseDestination(element, parserContext, builder, "topic");
		}
	}

	private void parseDestination(Element element, ParserContext parserContext, BeanDefinitionBuilder builder, String type) {
		boolean isPubSub = "topic".equals(type);
		String ref = element.getAttribute(type);
		String name = element.getAttribute(type + "-name");
		boolean isReference = StringUtils.hasText(ref);
		boolean isName = StringUtils.hasText(name);
		if (!(isReference ^ isName)) {
			parserContext.getReaderContext().error("Exactly one of the '" + type +
					"' or '" + type + "-name' attributes is required.", element);
		}
		if (isReference) {
			builder.addConstructorArgReference(ref);
		}
		else if (isName) {
			builder.addConstructorArgValue(name);
			builder.addConstructorArgValue(isPubSub);
			String destinationResolver = element.getAttribute("destination-resolver");
			if (StringUtils.hasText(destinationResolver)) {
				builder.addConstructorArgReference(destinationResolver);
			}
		}
	}

}
