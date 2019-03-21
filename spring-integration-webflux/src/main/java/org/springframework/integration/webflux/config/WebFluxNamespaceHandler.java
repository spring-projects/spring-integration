/*
 * Copyright 2017-2019 the original author or authors.
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

package org.springframework.integration.webflux.config;

import org.springframework.integration.config.xml.AbstractIntegrationNamespaceHandler;

/**
 * Namespace handler for Spring Integration's <em>webflux</em> namespace.
 *
 * @author Artem Bilan
 *
 * @since 5.0
 */
public class WebFluxNamespaceHandler extends AbstractIntegrationNamespaceHandler {

	public void init() {
		registerBeanDefinitionParser("inbound-channel-adapter", new WebFluxInboundEndpointParser(false));
		registerBeanDefinitionParser("inbound-gateway", new WebFluxInboundEndpointParser(true));
		registerBeanDefinitionParser("outbound-channel-adapter", new WebFluxOutboundChannelAdapterParser());
		registerBeanDefinitionParser("outbound-gateway", new WebFluxOutboundGatewayParser());
	}

}
