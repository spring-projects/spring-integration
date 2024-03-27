/*
 * Copyright 2002-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.integration.redis.config;

import org.springframework.integration.config.xml.AbstractIntegrationNamespaceHandler;

/**
 *  Namespace handler for Spring Integration's 'redis' namespace.
 *
 * @author Oleg Zhurakousky
 * @author Artem Bilan
 * @since 2.1
 */
public class RedisNamespaceHandler extends AbstractIntegrationNamespaceHandler {

	public void init() {
		registerBeanDefinitionParser("publish-subscribe-channel", new RedisChannelParser());
		registerBeanDefinitionParser("inbound-channel-adapter", new RedisInboundChannelAdapterParser());
		registerBeanDefinitionParser("store-inbound-channel-adapter", new RedisStoreInboundChannelAdapterParser());
		registerBeanDefinitionParser("store-outbound-channel-adapter", new RedisStoreOutboundChannelAdapterParser());
		registerBeanDefinitionParser("outbound-channel-adapter", new RedisOutboundChannelAdapterParser());
		registerBeanDefinitionParser("queue-inbound-channel-adapter", new RedisQueueInboundChannelAdapterParser());
		registerBeanDefinitionParser("queue-outbound-channel-adapter", new RedisQueueOutboundChannelAdapterParser());
		registerBeanDefinitionParser("outbound-gateway", new RedisOutboundGatewayParser());
		registerBeanDefinitionParser("queue-inbound-gateway", new RedisQueueInboundGatewayParser());
		registerBeanDefinitionParser("queue-outbound-gateway", new RedisQueueOutboundGatewayParser());
	}

}
