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

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.xml.ParserContext;
import org.springframework.integration.config.xml.IntegrationNamespaceUtils;
import org.w3c.dom.Element;


/**
 * @author Josh Long
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class TwitterNamespaceHandler extends org.springframework.beans.factory.xml.NamespaceHandlerSupport {


    public static String BASE_PACKAGE = "org.springframework.integration.twitter";

	public void init() {
		// twitter connections
		registerBeanDefinitionParser("twitter-connection", new ConnectionParser());

		// inbound
		registerBeanDefinitionParser("inbound-update-channel-adapter", new InboundTimelineUpdateEndpointParser());
		registerBeanDefinitionParser("inbound-dm-channel-adapter", new InboundDirectMessageEndpointParser());
		registerBeanDefinitionParser("inbound-mention-channel-adapter", new InboundMentionEndpointParser());

		// outbound
		registerBeanDefinitionParser("outbound-update-channel-adapter", new OutboundTimelineUpdateMessageHandlerParser());
		registerBeanDefinitionParser("outbound-dm-channel-adapter", new OutboundDirectMessageMessageHandlerParser());
	}

	public static void configureTwitterConnection(Element element, ParserContext parserContext, BeanDefinitionBuilder builder) {
		String ref = element.getAttribute("twitter-connection");

		if (org.springframework.util.StringUtils.hasText(ref)) {
			builder.addPropertyReference("twitterConnection", ref);
		} else {
			for (String attribute : new String[]{"consumer-key", "consumer-secret", "access-token", "access-token-secret"}) {
				IntegrationNamespaceUtils.setValueIfAttributeDefined(builder, element, attribute);
			}
		}
	}

}


	  