/*
 * Copyright 2002-2019 the original author or authors.
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

package org.springframework.integration.http.config;

import org.springframework.integration.config.xml.AbstractIntegrationNamespaceHandler;

/**
 * Namespace handler for Spring Integration's <em>http</em> namespace.
 *
 * @author Mark Fisher
 * @author Artem Bilan
 * @author Shiliang Li
 *
 * @since 1.0.2
 */
public class HttpNamespaceHandler extends AbstractIntegrationNamespaceHandler {

	public void init() {
		registerBeanDefinitionParser("inbound-channel-adapter", new HttpInboundEndpointParser(false));
		registerBeanDefinitionParser("inbound-gateway", new HttpInboundEndpointParser(true));
		registerBeanDefinitionParser("outbound-channel-adapter", new HttpOutboundChannelAdapterParser());
		registerBeanDefinitionParser("outbound-gateway", new HttpOutboundGatewayParser());
		registerBeanDefinitionParser("graph-controller", new IntegrationGraphControllerParser());
	}

}
