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

import org.springframework.integration.config.xml.AbstractIntegrationNamespaceHandler;

/**
 * Namespace handler for the Twitter adapters.
 * 
 * @author Josh Long
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class TwitterNamespaceHandler extends AbstractIntegrationNamespaceHandler {

	static String BASE_PACKAGE = "org.springframework.integration.twitter";


	public void init() {
		// inbound
		registerBeanDefinitionParser("inbound-channel-adapter", new TwitterReceivingMessageSourceParser());
		registerBeanDefinitionParser("dm-inbound-channel-adapter", new TwitterReceivingMessageSourceParser());
		registerBeanDefinitionParser("mentions-inbound-channel-adapter", new TwitterReceivingMessageSourceParser());
		registerBeanDefinitionParser("search-inbound-channel-adapter", new TwitterReceivingMessageSourceParser());

		// outbound
		registerBeanDefinitionParser("outbound-channel-adapter", new TwitterSendingMessageHandlerParser());
		registerBeanDefinitionParser("dm-outbound-channel-adapter", new TwitterSendingMessageHandlerParser());
	}

}
