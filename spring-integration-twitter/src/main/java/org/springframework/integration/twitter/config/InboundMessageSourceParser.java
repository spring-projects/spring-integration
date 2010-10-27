/*
 * Copyright 2010 the original author or authors
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

import org.springframework.beans.BeanMetadataElement;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.BeanDefinitionReaderUtils;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractPollingInboundChannelAdapterParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.w3c.dom.Element;

/**
 * A parser for InboundTimelineUpdateEndpoint endpoint. 
 * 
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class InboundMessageSourceParser extends AbstractPollingInboundChannelAdapterParser {

	@Override
	protected BeanMetadataElement parseSource(Element element, ParserContext parserContext) {  
		String elementName = element.getLocalName().trim();
		String className = null;
    	if ("inbound-update-channel-adapter".equals(elementName)){
    		className = BASE_PACKAGE +".inbound.TimelineUpdateMessageSource" ;
    	}
    	else if ("inbound-dm-channel-adapter".equals(elementName)){
    		className =  BASE_PACKAGE +  ".inbound.DirectMessageMessageSource"; 
    	}
    	else if ("inbound-mention-channel-adapter".equals(elementName)){
    		className = BASE_PACKAGE + ".inbound.MentionMessageSource";
    	}
    	else {
    		throw new IllegalArgumentException("Element '" + elementName + "' is not supported by this parser");
    	}
    	BeanDefinitionBuilder builder = BeanDefinitionBuilder.rootBeanDefinition(className);
    	IntegrationNamespaceUtils.setReferenceIfAttributeDefined(builder, element, "twitter-connection", "configuration");
    	String name = BeanDefinitionReaderUtils.registerWithGeneratedName(builder.getBeanDefinition(), parserContext.getRegistry());
		return new RuntimeBeanReference(name);
	}
}
