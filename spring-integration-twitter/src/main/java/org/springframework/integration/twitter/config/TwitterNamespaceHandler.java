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

import org.springframework.integration.config.xml.AbstractIntegrationNamespaceHandler;

/**
 * Namespace handler for the Twitter adapters.
 *
 * @author Josh Long
 * @author Oleg Zhurakousky
 * @author Gary Russell
 * @since 2.0
 */
public class TwitterNamespaceHandler extends AbstractIntegrationNamespaceHandler {

	@Override
	public void init() {
		// inbound
		registerBeanDefinitionParser("inbound-channel-adapter", new TwitterInboundChannelAdapterParser());
		registerBeanDefinitionParser("dm-inbound-channel-adapter", new TwitterInboundChannelAdapterParser());
		registerBeanDefinitionParser("mentions-inbound-channel-adapter", new TwitterInboundChannelAdapterParser());
		registerBeanDefinitionParser("search-inbound-channel-adapter", new TwitterInboundChannelAdapterParser());

		// outbound
		registerBeanDefinitionParser("outbound-channel-adapter", new TwitterOutboundChannelAdapterParser());
		registerBeanDefinitionParser("dm-outbound-channel-adapter", new TwitterOutboundChannelAdapterParser());
		registerBeanDefinitionParser("search-outbound-gateway", new TwitterSearchOutboundGatewayParser());
	}

}
