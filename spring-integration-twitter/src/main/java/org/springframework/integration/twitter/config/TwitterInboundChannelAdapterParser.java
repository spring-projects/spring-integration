/*
 * Copyright 2002-2014 the original author or authors
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

import org.w3c.dom.Element;

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractPollingInboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.integration.twitter.inbound.DirectMessageReceivingMessageSource;
import org.springframework.integration.twitter.inbound.MentionsReceivingMessageSource;
import org.springframework.integration.twitter.inbound.SearchReceivingMessageSource;
import org.springframework.integration.twitter.inbound.TimelineReceivingMessageSource;

/**
 * Parser for inbound Twitter Channel Adapters.
 *
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @since 2.0
 */
public class TwitterInboundChannelAdapterParser extends AbstractPollingInboundChannelAdapterParser {

	@Override
	protected BeanMetadataElement parseSource(Element element, ParserContext parserContext) {
		Class<?> clazz = determineClass(element, parserContext);
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(clazz);
		builder.addConstructorArgReference(element.getAttribute("twitter-template"));
		builder.addConstructorArgValue(element.getAttribute(ID_ATTRIBUTE));

		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "query");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "page-size");
		IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "metadata-store");
		return builder.getBeanDefinition();
	}


	private static Class<?> determineClass(Element element, ParserContext parserContext) {
		Class<?> clazz = null;
		String elementName = element.getLocalName().trim();
		if ("inbound-channel-adapter".equals(elementName)) {
			clazz = TimelineReceivingMessageSource.class;
		}
		else if ("dm-inbound-channel-adapter".equals(elementName)) {
			clazz = DirectMessageReceivingMessageSource.class;
		}
		else if ("mentions-inbound-channel-adapter".equals(elementName)) {
			clazz = MentionsReceivingMessageSource.class;
		}
		else if ("search-inbound-channel-adapter".equals(elementName)){
			clazz = SearchReceivingMessageSource.class;
		}
		else {
			parserContext.getReaderContext().error("element '" + elementName + "' is not supported by this parser.", element);
		}
		return clazz;
	}

}
