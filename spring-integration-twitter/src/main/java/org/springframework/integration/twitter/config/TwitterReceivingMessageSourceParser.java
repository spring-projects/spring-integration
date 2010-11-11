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

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractPollingInboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;

/**
 * Parser for inbound Twitter Channel Adapters.
 * 
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class TwitterReceivingMessageSourceParser extends AbstractPollingInboundChannelAdapterParser {

	@Override
	protected BeanMetadataElement parseSource(Element element, ParserContext parserContext) {
		String elementName = element.getLocalName().trim();
		String className = null;
		if ("inbound-channel-adapter".equals(elementName)) {
			className = BASE_PACKAGE + ".inbound.TimelineReceivingMessageSource";
		}
		else if ("dm-inbound-channel-adapter".equals(elementName)) {
			className = BASE_PACKAGE + ".inbound.DirectMessageReceivingMessageSource";
		}
		else if ("mentions-inbound-channel-adapter".equals(elementName)) {
			className = BASE_PACKAGE + ".inbound.MentionsReceivingMessageSource";
		}
		else {
			parserContext.getReaderContext().error("element '" + elementName + "' is not supported by this parser.", element);
		}
		BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(className);
		builder.addConstructorArgReference(element.getAttribute("twitter-template"));
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "auto-startup");
		String name = BeanDefinitionReaderUtils.registerWithGeneratedName(builder.getBeanDefinition(), parserContext.getRegistry());
		return new RuntimeBeanReference(name);
	}

}
