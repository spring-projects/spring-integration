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

package org.springframework.integration.mongodb.config;

import org.springframework.integration.config.xml.AbstractIntegrationNamespaceHandler;

/**
 *  Namespace handler for Spring Integration's 'mongodb' namespace.
 *
 * @author Amol Nayak
 * @author Oleg Zhurakousky
 *
 * @since 2.2
 */
public class MongoDbNamespaceHandler extends AbstractIntegrationNamespaceHandler {

	public void init() {
		registerBeanDefinitionParser("inbound-channel-adapter", new MongoDbInboundChannelAdapterParser());
		registerBeanDefinitionParser("outbound-channel-adapter", new MongoDbOutboundChannelAdapterParser());
		registerBeanDefinitionParser("outbound-gateway", new MongoDbOutboundGatewayParser());
	}

}
