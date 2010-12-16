/*
 * Copyright 2002-2010 the original author or authors
 *
 *     Licensed under the Apache License, Version 2.0 (the "License");
 *     you may not use this file except in compliance with the License.
 *     You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 *     Unless required by applicable law or agreed to in writing, software
 *     distributed under the License is distributed on an "AS IS" BASIS,
 *     WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *     See the License for the specific language governing permissions and
 *     limitations under the License.
 */

package org.springframework.integration.twitter.config;

import static org.springframework.integration.twitter.config.TwitterNamespaceHandler.BASE_PACKAGE;

import org.w3c.dom.Element;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractOutboundChannelAdapterParser;

/**
 * Parser for all outbound Twitter adapters
 * 
 * @author Josh Long
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class TwitterOutboundChannelAdapterParser extends AbstractOutboundChannelAdapterParser {

	@Override
	protected AbstractBeanDefinition parseConsumer(Element element, ParserContext parserContext) {
		String className = determineClassName(element, parserContext);
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(className);
		builder.addConstructorArgReference(element.getAttribute("twitter-template"));
		return builder.getBeanDefinition();
	}


	private static String determineClassName(Element element, ParserContext parserContext) {
		String className = null;
		String elementName = element.getLocalName().trim();
		if ("outbound-channel-adapter".equals(elementName)) {
			className = BASE_PACKAGE + ".outbound.StatusUpdatingMessageHandler";
		}
		else if ("dm-outbound-channel-adapter".equals(elementName)) {
			className = BASE_PACKAGE + ".outbound.DirectMessageSendingMessageHandler";
		}
		else {
			parserContext.getReaderContext().error("element '" + elementName + "' is not supported by this parser.", element);
		}
		return className;
	}

}
