/*
 * Copyright 2002-2016 the original author or authors.
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

package org.springframework.integration.amqp.config;

import org.springframework.integration.config.xml.AbstractIntegrationNamespaceHandler;

/**
 * Namespace handler for the AMQP schema.
 *
 * @author Mark Fisher
 * @author Gary Russell
 * @since 2.1
 */
public class AmqpNamespaceHandler extends AbstractIntegrationNamespaceHandler {

	@Override
	public void init() {
		this.registerBeanDefinitionParser("channel", new AmqpChannelParser());
		this.registerBeanDefinitionParser("publish-subscribe-channel", new AmqpChannelParser());
		this.registerBeanDefinitionParser("inbound-channel-adapter", new AmqpInboundChannelAdapterParser());
		this.registerBeanDefinitionParser("inbound-gateway", new AmqpInboundGatewayParser());
		this.registerBeanDefinitionParser("outbound-channel-adapter", new AmqpOutboundChannelAdapterParser());
		this.registerBeanDefinitionParser("outbound-gateway", new AmqpOutboundGatewayParser());
		this.registerBeanDefinitionParser("outbound-async-gateway", new AmqpOutboundGatewayParser());
	}

}
