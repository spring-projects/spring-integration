/*
 * Copyright 2002-2010 the original author or authors.
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

package org.springframework.integration.ip.config;

import org.springframework.integration.config.xml.AbstractIntegrationNamespaceHandler;

/**
 * Namespace handler for Spring Integration's <em>ip</em> namespace.
 * 
 * @author Gary Russell
 * @since 2.0
 */
public class IpNamespaceHandler extends AbstractIntegrationNamespaceHandler {

	public void init() {
		this.registerBeanDefinitionParser("inbound-channel-adapter", new IpInboundChannelAdapterParser());
		this.registerBeanDefinitionParser("outbound-channel-adapter", new IpOutboundChannelAdapterParser());
		this.registerBeanDefinitionParser("inbound-gateway", new IpInboundGatewayParser());
		this.registerBeanDefinitionParser("outbound-gateway", new IpOutboundGatewayParser());
		this.registerBeanDefinitionParser("tcp-connection-factory", new TcpConnectionParser());
		this.registerBeanDefinitionParser("tcp-inbound-channel-adapter", new TcpInboundChannelAdapterParser());
		this.registerBeanDefinitionParser("tcp-outbound-channel-adapter", new TcpOutboundChannelAdapterParser());
	}

}
