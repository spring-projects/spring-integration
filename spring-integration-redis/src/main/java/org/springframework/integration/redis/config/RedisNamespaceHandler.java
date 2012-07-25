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
package org.springframework.integration.redis.config;

import org.springframework.integration.config.xml.AbstractIntegrationNamespaceHandler;

/**
 *  Namespace handler for Spring Integration's 'redis' namespace.
 *
 * @author Oleg Zhurakousky
 * @since 2.1
 */
public class RedisNamespaceHandler extends AbstractIntegrationNamespaceHandler {

	public void init() {
		registerBeanDefinitionParser("publish-subscribe-channel", new RedisChannelParser());
		registerBeanDefinitionParser("inbound-channel-adapter", new RedisInboundChannelAdapterParser());
		registerBeanDefinitionParser("list-inbound-channel-adapter", new RedisCollectionsInboundChannelAdapterParser());
		registerBeanDefinitionParser("zset-inbound-channel-adapter", new RedisCollectionsInboundChannelAdapterParser());
		registerBeanDefinitionParser("outbound-channel-adapter", new RedisOutboundChannelAdapterParser());
	}

}
