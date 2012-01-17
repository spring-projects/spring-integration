/*
 * Copyright 2002-2012 the original author or authors.
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
package org.springframework.integration.jpa.config.xml;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.AbstractConsumerEndpointParser;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.springframework.util.StringUtils;
import org.w3c.dom.Element;

/**
 * The Parser for JPA Outbound Gateway, the MessageHandler implementation is same as the 
 * outbound chanel adapter and hence we extend te class and setting the few additional 
 * attributes that we wish to in the MessageSource
 * 
 * @author Amol Nayak
 * @since 2.2
 *
 */
public class JpaOutboundGatewayParser extends AbstractConsumerEndpointParser  {
	
	
	protected BeanDefinitionBuilder parseHandler(Element element,
			ParserContext parserContext) {
		BeanDefinitionBuilder builder = JpaParserUtils.getMessageHandlerBuilder(element, parserContext);
		//Now lets add the gateway specific atributes
		builder.addPropertyValue("produceReply", "true");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "select");
		IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, "max-rows-per-poll");
		
		String replyChannel = element.getAttribute("reply-channel");
		if (StringUtils.hasText(replyChannel)) {
			builder.addPropertyReference("outputChannel", replyChannel);
		}
		return builder;		
	}
	
	protected String getInputChannelAttributeName() {
		return "request-channel";
	}
	
}
